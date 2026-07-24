package com.netcracker.cloud.dbaas.client.arangodb.service;

import com.arangodb.ArangoDB;
import com.netcracker.cloud.dbaas.client.arangodb.classifier.ArangoDBClassifierBuilder;
import com.netcracker.cloud.dbaas.client.arangodb.entity.connection.ArangoConnection;
import com.netcracker.cloud.dbaas.client.arangodb.entity.database.ArangoDatabase;
import com.netcracker.cloud.dbaas.client.arangodb.test.configuration.TestArangoDBContainer;
import com.netcracker.cloud.dbaas.client.management.ArangoDatabaseProvider;
import com.netcracker.cloud.dbaas.client.management.DatabaseConfig;
import com.netcracker.cloud.dbaas.client.management.DatabasePool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;

import static com.netcracker.cloud.dbaas.client.arangodb.test.ArangoTestCommon.*;
import static com.netcracker.cloud.dbaas.client.arangodb.test.configuration.TestArangoDBConfiguration.DB_NAME_1;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Reproduces the stale-cached-connection hang: when an ArangoDB node goes silent
 * mid-session (TCP stays open, no response), checkConnection() blocks indefinitely
 * because CompletableFuture.get() never returns for async protocols (HTTP/2, VST with NIO).
 * The driver's socket-level timeout only applies to connection establishment, not to
 * in-flight requests over pre-existing pooled connections.
 * <p>
 * The two-phase TCP proxy completes a real ArangoDB handshake (phase 1: transparent),
 * then stops forwarding server responses while keeping the TCP connection alive
 * (phase 2: silent). This forces the driver to reuse a cached connection that will
 * never receive a reply — exactly what happens during an ArangoDB cluster failover.
 */
class StaleCachedConnectionHangTest {

    private static final int HANG_DETECTION_SECONDS = 10;
    private static final long CHECK_TIMEOUT_MS = 500;

    private TwoPhaseProxy proxy;

    @BeforeAll
    static void startContainer() {
        TestArangoDBContainer.getInstance().start();
    }

    @BeforeEach
    void startProxy() throws IOException {
        TestArangoDBContainer container = TestArangoDBContainer.getInstance();
        proxy = new TwoPhaseProxy(container.getHost(), container.getMappedPort(DB_PORT));
    }

    @AfterEach
    void stopProxy() throws IOException {
        proxy.close();
    }

    @Test
    void arangoDatabaseProvider_provide_shouldNotHangOnStaleCachedConnection() throws Exception {
        // timeout=0 disables the driver's socket timeout to prove it cannot rescue us:
        // the blocking point is CompletableFuture.get(), not a socket read
        ArangoDB driver = new ArangoDB.Builder()
                .host("127.0.0.1", proxy.getLocalPort())
                .user(TEST_USER)
                .password(TEST_PASSWORD)
                .timeout(0)
                .build();

        try (Closeable ignored = driver::shutdown) {
            // Warm up: one successful query so the driver caches the TCP connection in its pool
            driver.db(DB_NAME_1).query("RETURN 1", Integer.class).close();

            // Make the backend go silent — TCP stays up, responses stop arriving
            proxy.goSilent();

            ArangoConnection staleConnection = new ArangoConnection();
            staleConnection.setArangoDatabase(driver.db(DB_NAME_1));
            staleConnection.setArangoDatabaseAsync(driver.async().db(DB_NAME_1));

            ArangoDatabase staleDb = new ArangoDatabase();
            staleDb.setName(DB_NAME_1);
            staleDb.setConnectionProperties(staleConnection);

            DatabasePool pool = mock(DatabasePool.class);
            when(pool.getOrCreateDatabase(any(), any(), any())).thenReturn(staleDb);

            // retries=1, retryDelay=1: 0 would fall back to the provider's defaults (5 retries,
            // 5s delay) and blow past the hang-detection window used below.
            ArangoDatabaseProvider provider = new ArangoDatabaseProvider(
                    pool,
                    new ArangoDBClassifierBuilder(null),
                    DatabaseConfig.builder().build(),
                    1, 1, CHECK_TIMEOUT_MS
            );

            try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
                Future<?> future = executor.submit((Callable<?>) provider::provide);
                assertNotHang(future,
                        "ArangoDatabaseProvider.provide() hung on a stale cached connection. " +
                                "checkConnection() needs an explicit deadline independent of the " +
                                "driver's socket timeout.");
            }
        }
    }

    private void assertNotHang(Future<?> future, String failMessage) throws InterruptedException {
        try {
            future.get(HANG_DETECTION_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            fail(failMessage);
        } catch (ExecutionException e) {
            // Completed with an exception — did not hang
        }
    }

    private static class TwoPhaseProxy implements Closeable {

        private final ServerSocket serverSocket;
        private volatile boolean silent = false;

        TwoPhaseProxy(String targetHost, int targetPort) throws IOException {
            this.serverSocket = new ServerSocket(0);
            Thread acceptor = new Thread(() -> {
                while (!serverSocket.isClosed()) {
                    try {
                        Socket client = serverSocket.accept();
                        Socket backend = new Socket(targetHost, targetPort);
                        // client → backend: always forward so the driver can send requests
                        startForwarder("proxy-c2s", client.getInputStream(), backend.getOutputStream(), () -> false);
                        // backend → client: drop responses when silent, keeping TCP alive
                        startForwarder("proxy-s2c", backend.getInputStream(), client.getOutputStream(), () -> silent);
                    } catch (IOException ignored) {
                        // Accept loop ends when the socket is closed in close(); nothing to recover here
                    }
                }
            }, "proxy-acceptor");
            acceptor.setDaemon(true);
            acceptor.start();
        }

        void goSilent() {
            silent = true;
        }

        int getLocalPort() {
            return serverSocket.getLocalPort();
        }

        @Override
        public void close() throws IOException {
            serverSocket.close();
        }

        private void startForwarder(String name, InputStream in, OutputStream out, BooleanSupplier drop) {
            Thread t = new Thread(() -> {
                byte[] buf = new byte[4096];
                try {
                    int n;
                    while ((n = in.read(buf)) != -1) {
                        if (!drop.getAsBoolean()) {
                            out.write(buf, 0, n);
                            out.flush();
                        }
                    }
                } catch (IOException ignored) {
                    // Forwarding stops when either socket closes; the proxy is being torn down
                }
            }, name);
            t.setDaemon(true);
            t.start();
        }
    }
}
