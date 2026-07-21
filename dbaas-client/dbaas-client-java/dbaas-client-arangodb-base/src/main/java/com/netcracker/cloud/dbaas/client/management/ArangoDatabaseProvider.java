package com.netcracker.cloud.dbaas.client.management;

import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDatabase;
import com.netcracker.cloud.dbaas.client.arangodb.classifier.ArangoDBClassifierBuilder;
import com.netcracker.cloud.dbaas.client.arangodb.entity.connection.ArangoConnection;
import com.netcracker.cloud.dbaas.client.arangodb.entity.database.type.ArangoDBType;
import com.netcracker.cloud.dbaas.client.management.classifier.DbaaSChainClassifierBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RequiredArgsConstructor
@Slf4j
public class ArangoDatabaseProvider {

    private static final long DEFAULT_CHECK_TIMEOUT_MS = 60_000L;
    private static final ExecutorService CHECK_EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "arango-connection-check");
        t.setDaemon(true);
        return t;
    });

    private final DatabasePool pool;
    private final DbaaSChainClassifierBuilder builder;
    private final DatabaseConfig databaseConfig;

    private int retries = 0;
    private long retryDelay = 0;
    private long connectionCheckTimeoutMs = DEFAULT_CHECK_TIMEOUT_MS;

    public ArangoDatabaseProvider(DatabasePool pool, DbaaSChainClassifierBuilder builder, DatabaseConfig databaseConfig,
                                  int retries, long retryDelay) {
        this.pool = pool;
        this.builder = builder;
        this.databaseConfig = databaseConfig;
        this.retries = retries;
        this.retryDelay = retryDelay;
    }

    public ArangoDatabaseProvider(DatabasePool pool, DbaaSChainClassifierBuilder builder, DatabaseConfig databaseConfig,
                                  int retries, long retryDelay, long connectionCheckTimeoutMs) {
        this(pool, builder, databaseConfig, retries, retryDelay);
        this.connectionCheckTimeoutMs = connectionCheckTimeoutMs;
    }

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
        Future<Boolean> future = CHECK_EXECUTOR.submit(() -> {
            try (ArangoCursor<Integer> cursor = connection.getArangoDatabase().query("RETURN 42", Integer.class)) {
                Integer checkValue = cursor.next();
                if (checkValue == null || checkValue != 42)
                    throw new RuntimeException("Wrong check query result: " + checkValue);
                log.debug("Connection check succeeded, check value: {}", checkValue);
                return true;
            }
        });
        try {
            return future.get(connectionCheckTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            log.warn("Connection check timed out after {}ms", connectionCheckTimeoutMs);
            return false;
        } catch (Exception e) {
            log.debug("Connection check has failed with exception", e);
            return false;
        }
    }
}
