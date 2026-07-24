package com.netcracker.cloud.dbaas.client.management;

import com.arangodb.ArangoDatabase;
import com.netcracker.cloud.dbaas.client.arangodb.classifier.ArangoDBClassifierBuilder;
import com.netcracker.cloud.dbaas.client.arangodb.entity.connection.ArangoConnection;
import com.netcracker.cloud.dbaas.client.arangodb.entity.database.type.ArangoDBType;
import com.netcracker.cloud.dbaas.client.arangodb.service.ArangoConnectionChecker;
import com.netcracker.cloud.dbaas.client.management.classifier.DbaaSChainClassifierBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@RequiredArgsConstructor
@Slf4j
public class ArangoDatabaseProvider {
    /**
     * Deadline for the "RETURN 42" liveness probe, independent of the driver's own connect/request
     * timeout (see {@code dbaas.arangodb.timeout}) which is tuned for real, potentially slow queries.
     * A probe deadline that large would multiply by every retry and turn a down database into a
     * multi-minute block in {@link #provide()}.
     */
    public static final long DEFAULT_CONNECTION_CHECK_TIMEOUT_MS = 5_000L;

    private final DatabasePool pool;
    private final DbaaSChainClassifierBuilder builder;
    private final DatabaseConfig databaseConfig;

    private int retries = 0;
    private long retryDelay = 0;
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
     * @deprecated kept for source/binary compatibility with consumers built against the previous
     * 5-arg constructor; use {@link #ArangoDatabaseProvider(DatabasePool, DbaaSChainClassifierBuilder, DatabaseConfig, int, long, long)}.
     */
    @Deprecated
    public ArangoDatabaseProvider(DatabasePool pool, DbaaSChainClassifierBuilder builder,
                                  DatabaseConfig databaseConfig, int retries, long retryDelay) {
        this(pool, builder, databaseConfig, retries, retryDelay, DEFAULT_CONNECTION_CHECK_TIMEOUT_MS);
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
        if (checkConnection(connectionProperties)) {
            return connectionProperties.getArangoDatabase();
        }

        // retry <= retries: every recreated connection, including the last one, gets checked
        // before we declare exhaustion — a connection recreated right before the deadline is
        // just as usable as one recreated earlier.
        for (int retry = 0; retry <= retries; retry++) {
            if (retry > 0) {
                try {
                    Thread.sleep(retryDelay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            }
            log.debug("Retry #{}/{}", retry, retries);
            pool.removeCachedDatabase(ArangoDBType.INSTANCE, classifier);
            connectionProperties = pool.getOrCreateDatabase(ArangoDBType.INSTANCE, classifier, databaseConfig).getConnectionProperties();
            if (checkConnection(connectionProperties)) {
                return connectionProperties.getArangoDatabase();
            }
        }
        log.warn("Failed to get proper connection to DB after {} retries", retries);
        throw new IllegalStateException(
                "Failed to obtain a working ArangoDB connection after " + retries + " retries");
    }

    private boolean checkConnection(ArangoConnection connection) {
        return ArangoConnectionChecker.checkConnection(
                () -> connection.getArangoDatabase().arango().async().db(connection.getArangoDatabase().name())
                        .query("RETURN 42", Integer.class),
                connectionCheckTimeoutMs);
    }
}
