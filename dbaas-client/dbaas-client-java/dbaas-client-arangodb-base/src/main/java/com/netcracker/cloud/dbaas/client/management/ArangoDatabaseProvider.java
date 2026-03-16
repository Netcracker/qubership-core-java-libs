package com.netcracker.cloud.dbaas.client.management;

import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDatabase;
import com.netcracker.cloud.dbaas.client.arangodb.classifier.ArangoDBClassifierBuilder;
import com.netcracker.cloud.dbaas.client.arangodb.entity.connection.ArangoConnection;
import com.netcracker.cloud.dbaas.client.arangodb.entity.database.type.ArangoDBType;
import com.netcracker.cloud.dbaas.client.management.classifier.DbaaSChainClassifierBuilder;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@RequiredArgsConstructor
@Slf4j
public class ArangoDatabaseProvider {

    private final DatabasePool pool;
    private final DbaaSChainClassifierBuilder builder;
    private final DatabaseConfig databaseConfig;

    private int retries = 0;
    private long retryDelay = 0;

    public ArangoDatabase provide() {
        DbaasDbClassifier classifier = builder.build();
        return provide(classifier, databaseConfig);
    }

    public ArangoDatabase provide(String dbId) {
        return provide(dbId, databaseConfig);
    }

    public ArangoDatabase provide(String dbId, DatabaseConfig customDatabaseConfig) {
        DbaasDbClassifier classifier = new ArangoDBClassifierBuilder(builder).withDbId(dbId).build();
        return provide(classifier, customDatabaseConfig);
    }

    private ArangoDatabase provide(DbaasDbClassifier classifier, DatabaseConfig databaseConfig) {
        log.debug("Provide database with retries");
        ArangoConnection connectionProperties = pool.getOrCreateDatabase(ArangoDBType.INSTANCE, classifier, databaseConfig).getConnectionProperties();

        int retry = 0;
        if (!checkConnection(connectionProperties)) {
            pool.removeCachedDatabase(ArangoDBType.INSTANCE, classifier);
            connectionProperties = pool.getOrCreateDatabase(ArangoDBType.INSTANCE, classifier, databaseConfig).getConnectionProperties();

            while (retry < retries) {
                log.debug("Retry #{}/{}", retry + 1, retries);
                if (checkConnection(connectionProperties)) {
                    return connectionProperties.getArangoDatabase();
                } else {
                    try {
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                    pool.removeCachedDatabase(ArangoDBType.INSTANCE, classifier);
                    connectionProperties = pool.getOrCreateDatabase(ArangoDBType.INSTANCE, classifier, databaseConfig).getConnectionProperties();
                }
                retry++;
            }
            log.warn("Failed to get proper connection to DB");
        }
        return connectionProperties.getArangoDatabase();
    }

    private boolean checkConnection(ArangoConnection connection) {
        try {
            Integer checkValue;
            try (ArangoCursor<Integer> query = connection.getArangoDatabase().query("RETURN 42", Integer.class)) {
                checkValue = query.next();
                if (checkValue == null || checkValue != 42) throw new RuntimeException("Wrong check query result: " + checkValue);
            }
            log.debug("Connection check succeeded, check value: {}", checkValue);
            return true;
        } catch (Exception e) {
            log.debug("Connection check has failed with exception", e);
            return false;
        }
    }
}
