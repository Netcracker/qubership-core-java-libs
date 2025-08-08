package com.netcracker.cloud.dbaas.client.management;


import org.qubership.cloud.dbaas.client.DbaasClient;
import org.qubership.cloud.dbaas.client.DbaasConst;
import org.qubership.cloud.dbaas.client.entity.database.AbstractConnectorSettings;
import org.qubership.cloud.dbaas.client.entity.database.AbstractDatabase;
import org.qubership.cloud.dbaas.client.entity.database.type.DatabaseType;
import org.qubership.cloud.dbaas.client.service.LogicalDbProvider;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The pool to keep all databases created via dbaas client and there clients/connections
 */
@Slf4j
public class DatabasePool {

    public static final String MICROSERVICE_NAME_ENV = "MICROSERVICE_NAME";
    public static final String CLOUD_NAMESPACE_ENV = "CLOUD_NAMESPACE";
    private final DbaasClient dbaasClient;
    private final String microserviceName;
    private final String namespace;
    private List<PostConnectProcessor<?>> postProcessors;
    private DatabaseDefinitionHandler databaseDefinitionHandler;
    private final Comparator<Object> postConnectProcessorsOrder;
    private List<LogicalDbProvider> dbProviders;
    private Map<Class<? extends AbstractDatabase<?>>, DatabaseClientCreator<?, ?>> mapDatabaseClientCreators = new ConcurrentHashMap<>();
    /**
     * L1 cache holds cached databases connections. When client asks for a database, we first look in L1 cache.
     * Databases in this cache are ready-for-use, their post-processors have been already successfully applied.
     */
    private final Map<DatabaseKey<?, ?>, AbstractDatabase<?>> databasesCacheL1 = new ConcurrentHashMap<>();

    /**
     * This cache contains connection properties of the databases, that were obtained from DBaaS,
     * but which post-processors have failed. <br><br>
     * <p>
     * When client comes for a database, if we don't find it in L1 cache, we take database from L2 cache and try to
     * apply post-processors. If post-processors succeed, we save this database connection in L1 cache. <br><br>
     * <p>
     * If database is not present in L2, DBaaS Client gets new database from DBaaS and subscribes on notifications
     * about this database's changes. <br><br>
     */
    private final Map<DatabaseKey<?, ?>, AbstractDatabase<?>> databasesCacheL2 = new ConcurrentHashMap<>();

    public DatabasePool(DbaasClient dbaasClient,
                        String microserviceName,
                        String namespace,
                        List<PostConnectProcessor<?>> postProcessors,
                        DatabaseDefinitionHandler databaseDefinitionHandler) {
        this(dbaasClient,
                microserviceName,
                namespace,
                postProcessors, databaseDefinitionHandler, null, null, null);
    }


    public DatabasePool(DbaasClient dbaasClient,
                        String microserviceName,
                        String namespace,
                        List<PostConnectProcessor<?>> postProcessors,
                        DatabaseDefinitionHandler databaseDefinitionHandler,
                        Comparator<Object> postConnectProcessorsOrder,
                        List<LogicalDbProvider<?, ?>> dbProviders,
                        List<DatabaseClientCreator<?, ?>> databaseClientCreators) {
        this.dbaasClient = dbaasClient;
        this.microserviceName = microserviceName != null ? microserviceName : System.getenv(MICROSERVICE_NAME_ENV);
        this.namespace = namespace != null ? namespace : System.getenv(CLOUD_NAMESPACE_ENV);
        this.postProcessors = postProcessors == null ? Collections.emptyList() : postProcessors;
        this.databaseDefinitionHandler = databaseDefinitionHandler;
        this.postConnectProcessorsOrder = postConnectProcessorsOrder == null ? (o1, o2) -> 0 : postConnectProcessorsOrder;
        this.dbProviders = dbProviders != null ? sortProviders(dbProviders) : null;
        this.mapDatabaseClientCreators = databaseClientCreators != null ?
                databaseClientCreators.stream().collect(Collectors.toMap(DatabaseClientCreator::getSupportedDatabaseType, Function.identity())) :
                new HashMap<>();
    }

    public <T, D extends AbstractDatabase<T>> D getOrCreateDatabase(DatabaseType<T, D> dbType,
                                                                    DbaasDbClassifier dbaasDbClassifier) {
        return getOrCreateDatabase(dbType, dbaasDbClassifier, DatabaseConfig.builder().build());
    }

    public <T, D extends AbstractDatabase<T>> D getOrCreateDatabase(DatabaseType<T, D> dbType,
                                                                    DbaasDbClassifier dbaasDbClassifier,
                                                                    DatabaseConfig databaseConfig) {
        return getOrCreateDatabase(dbType, dbaasDbClassifier, databaseConfig, null);
    }

    public <T, D extends AbstractDatabase<T>, P extends AbstractConnectorSettings> D getOrCreateDatabase(DatabaseType<T, D> dbType,
                                                                                                         DbaasDbClassifier dbaasDbClassifier,
                                                                                                         DatabaseConfig databaseConfig,
                                                                                                         P settings) {
        Objects.requireNonNull(dbType);
        Objects.requireNonNull(dbaasDbClassifier);
        enrichClassifier(dbaasDbClassifier);
        DatabaseKey<T, D> key = new DatabaseKey<>(dbType, dbaasDbClassifier.asMap(), settings != null ? settings.getDiscriminator().getValue() : null);

        return (D) databasesCacheL1.computeIfAbsent(key, k -> loadDatabaseToL1Cache(key, databaseConfig, settings));
    }

    private void enrichClassifier(DbaasDbClassifier dbaasDbClassifier) {
        if (!dbaasDbClassifier.asMap().containsKey(DbaasConst.MICROSERVICE_NAME) && microserviceName != null) {
            dbaasDbClassifier.putProperty(DbaasConst.MICROSERVICE_NAME, microserviceName);
        }
        if (!dbaasDbClassifier.asMap().containsKey(DbaasConst.NAMESPACE) && namespace != null) {
            dbaasDbClassifier.putProperty(DbaasConst.NAMESPACE, namespace);
        }
    }

    private List<LogicalDbProvider> sortProviders(List<LogicalDbProvider<?, ?>> dbProviders) {
        return dbProviders.stream().sorted(Comparator.comparingInt(LogicalDbProvider::order)).collect(Collectors.toList());
    }

    public <T, D extends AbstractDatabase<T>> void removeCachedDatabase(DatabaseType<T, D> dbType,
                                                                        DbaasDbClassifier dbaasDbClassifier) {
        enrichClassifier(dbaasDbClassifier);
        removeCachedDatabase(new DatabaseKey<>(dbType, dbaasDbClassifier.asMap(), null));
    }

    @Deprecated(forRemoval = true) // do not use this method because the key's classifier may be not enriched
    public <T, D extends AbstractDatabase<T>> void removeCachedDatabase(DatabaseKey<T, D> key) {
        if (databasesCacheL2.containsKey(key)) {
            databasesCacheL2.remove(key);
            AbstractDatabase<?> oldDatabase = databasesCacheL1.remove(key);
            oldDatabase.setDoClose(true);
            closeConnection(oldDatabase);
            log.debug("Removed cached database for key {}", key);
        } else {
            log.debug("Couldn't find key for classifier {} and dbType {} in L2 cache while trying to remove cached database.", key.getClassifier(), key.getDbType());
        }
    }

    private <T, D extends AbstractDatabase<T>, P extends AbstractConnectorSettings> AbstractDatabase<?> loadDatabaseToL1Cache(DatabaseKey<T, D> key,
                                                                                                                              DatabaseConfig databaseConfig,
                                                                                                                              P settings) {
        AbstractDatabase<?> abstractDatabase;
        try {
            abstractDatabase = databasesCacheL2.computeIfAbsent(key, dbKey -> createDatabase(key, databaseConfig));
        } catch (Exception e) {
            log.error("Error while retrieving database from cache by key {}: {}", key, e);
            throw new RuntimeException("Failed to get or create database", e);
        }

        log.info("Created or received existing database: {}", abstractDatabase);
        DatabaseClientCreator<D, P> databaseClientCreator = (DatabaseClientCreator<D, P>) mapDatabaseClientCreators.get(key.getDbType().getDatabaseClass());
        if (databaseClientCreator != null) {
            log.debug("Running database client creators on db {}", abstractDatabase.getName());
            databaseClientCreator.create((D) abstractDatabase, settings);
        }
        log.debug("Running post connect processors on db {}", abstractDatabase.getName());
        try (AbstractDatabase<?> database = abstractDatabase) {
            database.setDoClose(true);
            applyPostConnectProcessors(database);
            database.setDoClose(false);

            return database;
        } catch (Exception e) {
            log.error("One of postprocessors has failed with error", e);
            throw new RuntimeException("PostProcessor error", e);
        }
    }

    protected <T, D extends AbstractDatabase<T>> D createDatabase(DatabaseKey<T, D> key, DatabaseConfig databaseConfig) {
        Map<String, Object> classifierFromKey = key.getClassifier();
        Map<String, Object> classifier = new HashMap<>(classifierFromKey);
        log.info("Creating Database for namespace:{}, with classifier: {} and configs {}",
                namespace, classifier, databaseConfig);

        D logDb = getDbFromProviders(classifier, databaseConfig, key.getDbType());
        if (logDb != null) {
            log.debug("Logical database was obtained from custom logical db provider. Classifier: {}, type {}", classifier, key.getDbType());
            return logDb;
        }

        databaseDefinitionHandler.applyDefinitionProcess(key.getDbType(), databaseConfig, classifier, namespace);
        return dbaasClient.getOrCreateDatabase(
                key.getDbType(),
                namespace,
                classifier,
                databaseConfig);
    }

    private Comparator<Object> getComparator() {
        return postConnectProcessorsOrder;
    }

    private <T, D extends AbstractDatabase<T>> void applyPostConnectProcessors(D database) {
        List<PostConnectProcessor<D>> postProcessorsForConnection = postProcessors.stream()
                .filter(postConnectProcessor -> postConnectProcessor.getSupportedDatabaseType() != null)
                .filter(postConnectProcessor -> postConnectProcessor.getSupportedDatabaseType().isInstance(database))
                .map(postConnectProcessor -> (PostConnectProcessor<D>) postConnectProcessor)
                .sorted(getComparator())
                .collect(Collectors.toList());

        if (postProcessorsForConnection.isEmpty()) {
            log.debug("No postprocessor was found for connection of db type: {}. Skip postprocessing for the connection.", database.getClass());
        } else {
            log.debug("Found postprocessor(s) for connection of db type: {}. Starting postprocessing for the connection.", database.getClass());
            postProcessorsForConnection.forEach(processor -> processor.process(database));
            log.debug("Finished postprocessing");
        }
    }

    private <T, D extends AbstractDatabase<T>> D getDbFromProviders(Map<String, Object> classifier,
                                                                    DatabaseConfig databaseConfig,
                                                                    DatabaseType<T, D> type) {

        log.debug("Trying to get DB from providers {}", dbProviders);
        if (dbProviders != null) {
            SortedMap<String, Object> sortedClassifier = new TreeMap<>(classifier);
            for (LogicalDbProvider dbProvider : dbProviders) {
                if (dbProvider.getSupportedDatabaseType().isAssignableFrom(type.getDatabaseClass())) {
                    D database = (D) dbProvider.provide(sortedClassifier, databaseConfig, namespace);
                    if (database != null) {
                        return database;
                    }
                }
            }
        }
        return null;
    }

    private void closeConnection(AutoCloseable connection) {
        try {
            connection.close();
            log.info("Closed connection for connection: {}", connection);
        } catch (Exception e) {
            log.error("Failed to close connection: " + connection, e);
        }
    }

    private void closeAllDatabasesConnections(Map<DatabaseKey<?, ?>, AbstractDatabase<?>> databases) {
        databases.values().stream()
                .filter(abstractDatabase -> abstractDatabase.getConnectionProperties() instanceof AutoCloseable)
                .map(abstractDatabase -> (AutoCloseable) abstractDatabase.getConnectionProperties())
                .forEach(this::closeConnection);
    }

    @PreDestroy
    private void closeDatabaseConnections() {
        log.info("Close all database connections...");
        closeAllDatabasesConnections(databasesCacheL2);
        databasesCacheL2.clear();
        databasesCacheL1.clear();
    }
}
