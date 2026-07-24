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

    public static final int DEFAULT_RETRIES = 5;
    public static final long DEFAULT_RETRY_DELAY_MS = 5_000L;

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
     * @deprecated kept for source/binary compatibility with consumers built against the previous
     * 5-arg constructor; use {@link #ArangoDatabaseProvider(DatabasePool, DbaaSChainClassifierBuilder, DatabaseConfig, int, long, long)}.
     */
    @Deprecated
    public ArangoDatabaseProvider(DatabasePool pool, DbaaSChainClassifierBuilder builder,
                                  DatabaseConfig databaseConfig, int retries, long retryDelay) {
        this(pool, builder, databaseConfig, retries, retryDelay, DEFAULT_CONNECTION_CHECK_TIMEOUT_MS);
    }

    /**
     * Derives a variant of this provider with a different retry policy, sharing the same pool,
     * classifier builder and database config. Useful for callers that hold a lock or otherwise
     * need a bounded worst-case wait (e.g. {@code DbaasArangoTemplate}'s write-lock-holding
     * reconnect), independent of the retry policy configured for direct {@link #provide()} calls.
     */
    public ArangoDatabaseProvider withRetryPolicy(int retries, long retryDelay, long connectionCheckTimeoutMs) {
        return new ArangoDatabaseProvider(pool, builder, databaseConfig, retries, retryDelay, connectionCheckTimeoutMs);
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
            log.debug("Retry #{}/{}", retry + 1, retries);
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
        try {
            return ArangoConnectionChecker.checkConnection(
                    () -> connection.getArangoDatabase().arango().async().db(connection.getDbName())
                            .query("RETURN 42", Integer.class),
                    connectionCheckTimeoutMs);
        } catch (InterruptedException e) {
            // Matches DbaasArangoTemplate.checkConnection() and pre-existing (pre-PR) behavior,
            // where any exception during the check — interrupt included — was swallowed and
            // treated as unhealthy. The flag is restored so it stays observable by provide()'s
            // own retry loop (e.g. the Thread.sleep() between retries aborts immediately).
            Thread.currentThread().interrupt();
            log.debug("Connection check was interrupted", e);
            return false;
        }
    }
}
