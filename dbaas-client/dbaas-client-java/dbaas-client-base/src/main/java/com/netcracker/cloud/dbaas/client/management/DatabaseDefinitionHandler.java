package com.netcracker.cloud.dbaas.client.management;


import com.netcracker.cloud.dbaas.client.DbaasClient;
import com.netcracker.cloud.dbaas.client.annotaion.Role;
import com.netcracker.cloud.dbaas.client.entity.database.AbstractDatabase;
import com.netcracker.cloud.dbaas.client.entity.database.type.DatabaseType;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Deprecated(forRemoval = true)
public final class DatabaseDefinitionHandler {

    private Map<Class<? extends AbstractDatabase<?>>, DatabaseDefinitionProcessor<?>> mapDatabaseDefinitionProcessors;
    private Map<Class<? extends AbstractDatabase<?>>, DatabaseClientCreator<?, ?>> mapDatabaseClientCreators;
    private final DbaasClient dbaasClient;

    @SuppressWarnings("unchecked")
    @Deprecated(forRemoval = true)
    public DatabaseDefinitionHandler(Optional<List<DatabaseDefinitionProcessor<?>>> databaseDefinitionProcessors,
                                     Optional<List<DatabaseClientCreator<?, ?>>> databaseClientCreators,
                                     DbaasClient dbaasClient) {

        this.mapDatabaseDefinitionProcessors = databaseDefinitionProcessors
                .map(definitionProcessors -> definitionProcessors.stream().collect(Collectors.toMap(DatabaseDefinitionProcessor::getSupportedDatabaseType, Function.identity())))
                .orElse(Collections.EMPTY_MAP);
        this.mapDatabaseClientCreators = databaseClientCreators
                .map(clientCreators -> clientCreators.stream().collect(Collectors.toMap(DatabaseClientCreator::getSupportedDatabaseType, Function.identity())))
                .orElse(Collections.EMPTY_MAP);
        this.dbaasClient = dbaasClient;
    }

    @Deprecated(forRemoval = true)
    public <D extends AbstractDatabase<T>, T> void applyDefinitionProcess(DatabaseType<T, D> dbType,
                                                                          DatabaseConfig databaseConfig,
                                                                          Map<String, Object> classifier,
                                                                          String namespace) {
        Class<? extends D> databaseClass = dbType.getDatabaseClass();
        DatabaseDefinitionProcessor definitionProcessor = mapDatabaseDefinitionProcessors.get(databaseClass);
        if (definitionProcessor != null) {
            if (!mapDatabaseClientCreators.containsKey(databaseClass)) {
                throw new RuntimeException("database client creator is not found for: " + databaseClass.getName());
            }
            log.info("database definition process is found for: {}", databaseClass.getName());
            process(definitionProcessor, dbType, databaseConfig, classifier, namespace);
        }
    }

    @Deprecated(forRemoval = true)
    private <D extends AbstractDatabase<T>, T> void process(DatabaseDefinitionProcessor<D> definitionProcessor,
                                                            DatabaseType<T, D> dbType,
                                                            DatabaseConfig databaseConfig,
                                                            Map<String, Object> classifier,
                                                            String namespace) {
        String definitionRole = getRole(definitionProcessor);
        String runtimeUser = databaseConfig.getUserRole();
        databaseConfig.setUserRole(definitionRole);
        log.debug("get database for definition process. Classifier {}, databaseConfig {}", classifier, databaseConfig);
        D database = dbaasClient.getOrCreateDatabase(dbType, namespace, classifier, databaseConfig);

        DatabaseClientCreator<D, ?> clientCreator = (DatabaseClientCreator<D, ?>) mapDatabaseClientCreators.get(dbType.getDatabaseClass());
        clientCreator.create(database);
        log.debug("perform database definition process for db: {}", database);
        definitionProcessor.process(database);
        if (database.getConnectionProperties() instanceof AutoCloseable) {
            closeConnection((AutoCloseable) database.getConnectionProperties());
        }
        databaseConfig.setUserRole(runtimeUser);
    }

    private void closeConnection(AutoCloseable connection) {
        try {
            connection.close();
        } catch (Exception e) {
            log.error("Failed to close connection: " + connection, e);
        }
    }


    private String getRole(DatabaseDefinitionProcessor<?> definitionProcessor) {
        return Arrays.stream(definitionProcessor.getClass().getMethods())
                .filter(method -> method.isAnnotationPresent(Role.class))
                .map(method -> method.getAnnotation(Role.class).value())
                .findFirst()
                .orElse("admin");
    }

}
