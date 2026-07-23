package com.netcracker.cloud.dbaas.client.management;

import com.arangodb.ArangoCursorAsync;
import com.arangodb.ArangoDatabase;
import com.netcracker.cloud.dbaas.client.arangodb.classifier.ArangoDBClassifierBuilder;
import com.netcracker.cloud.dbaas.client.arangodb.entity.connection.ArangoConnection;
import com.netcracker.cloud.dbaas.client.arangodb.entity.database.type.ArangoDBType;
import com.netcracker.cloud.dbaas.client.management.classifier.DbaaSChainClassifierBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RequiredArgsConstructor
@Slf4j
public class ArangoDatabaseProvider {

    private final DatabasePool pool;
    private final DbaaSChainClassifierBuilder builder;
    private final DatabaseConfig databaseConfig;

    private int retries = 0;
    private long retryDelay = 0;
    private long connectionCheckTimeoutMs;
    public ArangoDatabaseProvider(DatabasePool pool, DbaaSChainClassifierBuilder builder, DatabaseConfig databaseConfig,
                                  int retries, long retryDelay, long connectionCheckTimeoutMs) {
        this.pool = pool;
        this.builder = builder;
        this.databaseConfig = databaseConfig;
        this.retries = retries;
        this.retryDelay = retryDelay;
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
            log.warn("Failed to get proper connection to DB after {} retries", retries);
            throw new IllegalStateException(
                    "Failed to obtain a working ArangoDB connection after " + retries + " retries");
        }
        return connectionProperties.getArangoDatabase();
    }

    private boolean checkConnection(ArangoConnection connection) {
        try {
            CompletableFuture<ArangoCursorAsync<Integer>> future =
                    connection.getArangoDatabaseAsync().query("RETURN 42", Integer.class);
            // Best-effort release of a cursor that arrives after we've given up. close() is async
            // (returns a CompletableFuture and doesn't block); we drop that future on purpose —
            // awaiting it could block on the same silent socket. Eviction force-closes the whole driver anyway.
            future.whenComplete((cursor, err) -> {
                if (cursor != null) {
                    cursor.close();
                }
            });
            ArangoCursorAsync<Integer> cursor = future.get(connectionCheckTimeoutMs, TimeUnit.MILLISECONDS);
            Integer checkValue = cursor.getResult().iterator().next();
            boolean ok = checkValue != null && checkValue == 42;
            if (ok) {
                log.debug("Connection check succeeded, check value: {}", checkValue);
            } else {
                log.warn("Wrong check query result: {}", checkValue);
            }
            return ok;
        } catch (TimeoutException e) {
            log.warn("Connection check timed out after {}ms", connectionCheckTimeoutMs);
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Connection check interrupted", e);
        } catch (ExecutionException | RuntimeException e) {
            log.debug("Connection check failed", e);
            return false;
        }
    }
}
