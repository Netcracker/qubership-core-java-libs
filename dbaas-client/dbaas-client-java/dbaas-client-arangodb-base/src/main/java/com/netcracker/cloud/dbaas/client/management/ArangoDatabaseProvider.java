package com.netcracker.cloud.dbaas.client.management;

import com.arangodb.ArangoDatabase;
import com.netcracker.cloud.dbaas.client.arangodb.classifier.ArangoDBClassifierBuilder;
import com.netcracker.cloud.dbaas.client.arangodb.entity.connection.ArangoConnection;
import com.netcracker.cloud.dbaas.client.arangodb.entity.database.type.ArangoDBType;
import com.netcracker.cloud.dbaas.client.arangodb.service.ArangoConnectionChecker;
import com.netcracker.cloud.dbaas.client.management.classifier.DbaaSChainClassifierBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static com.netcracker.cloud.dbaas.client.entity.DbaasApiProperties.DEFAULT_RETRIES;
import static com.netcracker.cloud.dbaas.client.entity.DbaasApiProperties.DEFAULT_RETRY_DELAY_MS;

@RequiredArgsConstructor
@Slf4j
public class ArangoDatabaseProvider {

    public static final long DEFAULT_CONNECTION_CHECK_TIMEOUT_MS = 60_000L;

    private final DatabasePool pool;
    private final DbaaSChainClassifierBuilder builder;
    private final DatabaseConfig databaseConfig;

    private int retries = DEFAULT_RETRIES;
    private long retryDelay = DEFAULT_RETRY_DELAY_MS;
    private long connectionCheckTimeoutMs = DEFAULT_CONNECTION_CHECK_TIMEOUT_MS;

    public ArangoDatabaseProvider(DatabasePool pool, DbaaSChainClassifierBuilder builder, DatabaseConfig databaseConfig,
                                  int retries, long retryDelay, long connectionCheckTimeoutMs) {
        this.pool = pool;
        this.builder = builder;
        this.databaseConfig = databaseConfig;
        this.retries = retries;
        this.retryDelay = retryDelay;
        this.connectionCheckTimeoutMs = connectionCheckTimeoutMs;
    }

    /**
     * If the initial connection fails its health check and all reconnect retries are exhausted
     * without producing a healthy connection, throws {@link IllegalStateException}.
     *
     * @throws IllegalStateException if no working connection could be obtained
     */
    public ArangoDatabase provide() {
        DbaasDbClassifier classifier = builder.build();
        return provide(classifier, databaseConfig);
    }

    /** @throws IllegalStateException if no working connection could be obtained, see {@link #provide()} */
    public ArangoDatabase provide(String dbId) {
        return provide(dbId, databaseConfig);
    }

    /** @throws IllegalStateException if no working connection could be obtained, see {@link #provide()} */
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
            log.warn("Failed to get proper connection to DB after {} retries", retries);
            throw new IllegalStateException(
                    "Failed to obtain a working ArangoDB connection after " + retries + " retries");
        }
        return connectionProperties.getArangoDatabase();
    }

    private boolean checkConnection(ArangoConnection connection) {
        try {
            return ArangoConnectionChecker.checkConnection(
                    () -> connection.getArangoDatabaseAsync().query("RETURN 42", Integer.class),
                    connectionCheckTimeoutMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Connection check interrupted", e);
        }
    }
}
