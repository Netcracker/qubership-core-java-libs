package com.netcracker.cloud.dbaas.client.management;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

@Slf4j
public class ArangoConnectionChecker {

    private static final ExecutorService EXECUTOR = new ThreadPoolExecutor(
            5, 5, 0L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(),
            r -> { Thread t = new Thread(r, "arango-connection-check"); t.setDaemon(true); return t; });

    private ArangoConnectionChecker() {}

    public static boolean check(Callable<Boolean> check, long timeoutMs) {
        Future<Boolean> future = EXECUTOR.submit(check);
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            log.warn("Connection check timed out after {}ms", timeoutMs);
            return false;
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            log.debug("Connection check was interrupted", e);
            return false;
        } catch (Exception e) {
            log.debug("Connection check failed with exception", e);
            return false;
        }
    }
}