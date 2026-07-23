package com.netcracker.cloud.dbaas.client.management;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netcracker.cloud.dbaas.client.DbaasClient;
import com.netcracker.cloud.dbaas.client.DbaasConst;
import com.netcracker.cloud.dbaas.client.entity.database.AbstractConnectorSettings;
import com.netcracker.cloud.dbaas.client.entity.database.AbstractDatabase;
import com.netcracker.cloud.dbaas.client.entity.database.type.DatabaseType;
import com.netcracker.cloud.dbaas.client.service.LogicalDbProvider;
import com.netcracker.cloud.dbaas.client.service.mountedsecret.MountedSecretSource;
import com.netcracker.cloud.dbaas.client.service.mountedsecret.SecretMetadata;
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
     * Reads connection properties from Secrets mounted at {@code /etc/secrets/dbaas-secrets},
     * consulted before the REST call in {@link #createDatabase}. Always registered; when nothing is
     * mounted it returns empty and the pool falls back to REST exactly as before.
     */
    private MountedSecretSource mountedSecretSource = new MountedSecretSource();

    /**
     * Used to build a typed {@link AbstractDatabase} from a mounted Secret via the synthetic-response
     * mechanism (a property map converted to {@code DatabaseType#getDatabaseClass()}). Unknown
     * connection-property keys (e.g. {@code roHost}) are tolerated, matching the REST deserialization.
     */
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /**
     * L1 cache holds cached databases connections. When client asks for a database, we first look in L1 cache.
     * Databases in this cache are ready-for-use, their post-processors have been already successfully applied.
     */
    private final Map<DatabaseKey<?, ?>, AbstractDatabase<?>> databasesCacheL1 = new ConcurrentHashMap<>();

    /**
     * L2 cache holds databases whose initialization — the database client creator and the
     * post-connect processors — has fully succeeded. <br><br>
     * <p>
     * When client comes for a database and it is missing from L1, we take it from here and
     * re-initialize it instead of requesting DBaaS again. A database whose initialization
     * failed is closed and deliberately not cached, so the next request starts from a fresh
     * DBaaS lookup instead of reusing a closed connection. <br><br>
     * <p>
     * If database is not present in L2 either, DBaaS Client gets a new database from DBaaS. <br><br>
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
            abstractDatabase = databasesCacheL2.get(key);
            if (abstractDatabase == null) {
                abstractDatabase = createDatabase(key, databaseConfig);
            }
        } catch (Exception e) {
            log.error("Error while retrieving database from cache by key {}", key, e);
            throw new RuntimeException("Failed to get or create database", e);
        }

        log.info("Created or received existing database: {}", abstractDatabase);
        // The client creator runs inside try-with-resources on purpose: it may allocate live
        // resources (e.g. a connection pool) before failing, and a database that did not finish
        // initialization must be closed and must not reach the cache.
        try (AbstractDatabase<?> database = abstractDatabase) {
            database.setDoClose(true);
            DatabaseClientCreator<D, P> databaseClientCreator = (DatabaseClientCreator<D, P>) mapDatabaseClientCreators.get(key.getDbType().getDatabaseClass());
            if (databaseClientCreator != null) {
                log.debug("Running database client creators on db {}", database.getName());
                databaseClientCreator.create((D) database, settings);
            }
            log.debug("Running post connect processors on db {}", database.getName());
            applyPostConnectProcessors(database);
            database.setDoClose(false);

            databasesCacheL2.put(key, abstractDatabase);

            return database;
        } catch (Exception e) {
            log.error("Database client creator or post-connect processor failed; the connection is closed and not cached", e);
            throw new RuntimeException("Database initialization error", e);
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

        D mountedDb = getDbFromMountedSecret(classifier, databaseConfig, key.getDbType());
        if (mountedDb != null) {
            log.debug("Logical database was obtained from mounted secret. Classifier: {}, type {}", classifier, key.getDbType());
            return mountedDb;
        }

        databaseDefinitionHandler.applyDefinitionProcess(key.getDbType(), databaseConfig, classifier, namespace);
        return dbaasClient.getOrCreateDatabase(
                key.getDbType(),
                namespace,
                classifier,
                databaseConfig);
    }

    private <T, D extends AbstractDatabase<T>> D getDbFromMountedSecret(Map<String, Object> classifier,
                                                                        DatabaseConfig databaseConfig,
                                                                        DatabaseType<T, D> type) {
        String role = databaseConfig != null ? databaseConfig.getUserRole() : null;
        return mountedSecretSource.resolve(classifier, type.getName(), role)
                .map(resolved -> buildAbstractDatabase(type, classifier, resolved))
                .orElse(null);
    }

    /**
     * Builds the typed database from a mounted Secret (synthetic-response): assemble a map mirroring
     * the dbaas REST response and convert it to {@code type.getDatabaseClass()} with the same
     * deserialization semantics as the REST path. No provisioning and no REST call happen here.
     */
    private <T, D extends AbstractDatabase<T>> D buildAbstractDatabase(DatabaseType<T, D> type,
                                                                       Map<String, Object> classifier,
                                                                       MountedSecretSource.Resolved resolved) {
        SecretMetadata meta = resolved.metadata();
        Map<String, Object> synthetic = new HashMap<>();
        synthetic.put("classifier", meta.getClassifier() != null ? meta.getClassifier() : classifier);
        synthetic.put("connectionProperties", resolved.connectionProperties());

        String name = meta.getName() != null ? meta.getName() : asString(resolved.connectionProperties().get("name"));
        if (name != null) {
            synthetic.put("name", name);
        }
        String dbNamespace = meta.getNamespace() != null ? meta.getNamespace() : asString(classifier.get(DbaasConst.NAMESPACE));
        if (dbNamespace != null) {
            synthetic.put("namespace", dbNamespace);
        }
        if (meta.getSettings() != null) {
            synthetic.put("settings", meta.getSettings());
        }
        return objectMapper.convertValue(synthetic, type.getDatabaseClass());
    }

    private static String asString(Object value) {
        return value instanceof String s ? s : null;
    }

    /**
     * Test seam: package-private so unit tests can point the mounted-secret source at a temp directory.
     */
    void setMountedSecretSource(MountedSecretSource mountedSecretSource) {
        this.mountedSecretSource = mountedSecretSource;
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
