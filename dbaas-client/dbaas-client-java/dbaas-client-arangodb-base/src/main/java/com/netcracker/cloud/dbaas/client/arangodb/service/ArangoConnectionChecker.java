package com.netcracker.cloud.dbaas.client.arangodb.service;

import com.arangodb.ArangoCursorAsync;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * Shared "RETURN 42" liveness probe used by both {@code ArangoDatabaseProvider} and
 * {@code DbaasArangoTemplate} to bound a connection check with an explicit deadline
 * independent of the driver's own socket timeout (which does not apply to in-flight
 * requests over pre-existing pooled connections).
 * <p>
 * Interrupt handling is intentionally left to the caller: this method propagates
 * {@link InterruptedException} without restoring the interrupt flag or converting it to a
 * boolean result, since the two callers disagree on the right response (abort vs. treat as a
 * failed check).
 */
@Slf4j
public final class ArangoConnectionChecker {

    private ArangoConnectionChecker() {
    }

    public static boolean checkConnection(Supplier<CompletableFuture<ArangoCursorAsync<Integer>>> queryProbe, long timeoutMs) throws InterruptedException {
        try {
            CompletableFuture<ArangoCursorAsync<Integer>> future = queryProbe.get();
            // Best-effort release of a cursor that arrives after we've given up. close() is async
            // (returns a CompletableFuture and doesn't block); we drop that future on purpose —
            // awaiting it could block on the same silent socket. Eviction force-closes the whole driver anyway.
            future.whenComplete((cursor, err) -> {
                if (cursor != null) {
                    cursor.close();
                }
            });
            ArangoCursorAsync<Integer> cursor = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            Integer checkValue = cursor.getResult().iterator().next();
            boolean ok = checkValue != null && checkValue == 42;
            if (ok) {
                log.debug("Connection check succeeded, check value: {}", checkValue);
            } else {
                log.warn("Wrong check query result: {}", checkValue);
            }
            return ok;
        } catch (TimeoutException e) {
            log.warn("Connection check timed out after {}ms", timeoutMs);
            return false;
        } catch (ExecutionException | RuntimeException e) {
            log.debug("Connection check failed", e);
            return false;
        }
    }
}