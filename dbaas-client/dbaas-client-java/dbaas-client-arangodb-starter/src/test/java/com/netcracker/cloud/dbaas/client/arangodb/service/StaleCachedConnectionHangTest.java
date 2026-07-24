package com.netcracker.cloud.dbaas.client.arangodb.service;

import com.arangodb.ArangoDB;
import com.netcracker.cloud.dbaas.client.arangodb.classifier.ArangoDBClassifierBuilder;

import com.netcracker.cloud.dbaas.client.arangodb.configuration.DbaasArangoDBConfigurationProperties;
import com.netcracker.cloud.dbaas.client.arangodb.entity.connection.ArangoConnection;
import com.netcracker.cloud.dbaas.client.arangodb.entity.database.ArangoDatabase;
import com.netcracker.cloud.dbaas.client.arangodb.test.configuration.TestArangoDBContainer;
import com.netcracker.cloud.dbaas.client.arangodb.util.ArangoTemplateCreationUtils;
import com.netcracker.cloud.dbaas.client.management.ArangoDatabaseProvider;
import com.netcracker.cloud.dbaas.client.management.DatabaseConfig;
import com.netcracker.cloud.dbaas.client.management.DatabasePool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;

import static com.netcracker.cloud.dbaas.client.arangodb.test.ArangoTestCommon.*;
import static com.netcracker.cloud.dbaas.client.arangodb.test.configuration.TestArangoDBConfiguration.DB_NAME_1;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Reproduces the stale-cached-connection hang: when an ArangoDB node goes silent
 * mid-session (TCP stays open, no response), checkConnection() blocks indefinitely
 * because CompletableFuture.get() never returns for async protocols (HTTP/2, VST with NIO).
 * <p>
 * Only true when the driver's own {@code timeout} is 0 (disabled), which is what the first
 * test below configures on purpose. With a positive {@code timeout}, driver 7.26.0's
 * {@code HttpConnection} feeds the same value into both the connect timeout and the
 * Vert.x {@code WebClientOptions} idle timeout, so it already bounds in-flight requests over
 * a pooled connection too — see {@link #dbaasArangoTemplate_query_shouldNotHangOnStaleCachedConnection_andRecover()}
 * below, which uses a positive timeout and exercises {@code DbaasArangoTemplate.wrapWithRetry()}
 * end to end instead of a black-hole/fresh-connection path.
 * <p>
 * The two-phase TCP proxy completes a real ArangoDB handshake (phase 1: transparent),
 * then stops forwarding server responses while keeping the TCP connection alive
 * (phase 2: silent). This forces the driver to reuse a cached connection that will
 * never receive a reply — exactly what happens during an ArangoDB cluster failover.
 */
class StaleCachedConnectionHangTest {

    private static final int HANG_DETECTION_SECONDS = 10;
    private static final long CHECK_TIMEOUT_MS = 500;
    private static final int DRIVER_TIMEOUT_MS = 500;

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
            staleConnection.setDbName(DB_NAME_1);
            staleConnection.setArangoDatabase(driver.db(DB_NAME_1));

            ArangoDatabase staleDb = new ArangoDatabase();
            staleDb.setName(DB_NAME_1);
            staleDb.setConnectionProperties(staleConnection);

            DatabasePool pool = mock(DatabasePool.class);
            when(pool.getOrCreateDatabase(any(), any(), any())).thenReturn(staleDb);

            // retries=1, retryDelay=1: small but non-zero so a real retry happens without waiting
            // on the production retry delay.
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

    /**
     * Covers the template's actual query path (not just checkConnection): a positive driver
     * {@code timeout} against a connection that goes stale <em>after</em> it's already cached and
     * live, exercising {@code DbaasArangoTemplate.wrapWithRetry()} end to end. Unlike
     * {@code CheckConnectionTimeoutTest.dbaasArangoTemplate_query_shouldNotHang} (a fresh
     * connection to a black hole — the connection-establishment path), this proxies a real
     * ArangoDB handshake and only goes silent afterwards, so the driver is reusing a pooled
     * connection exactly like the failover scenario above.
     */
    @Test
    void dbaasArangoTemplate_query_shouldNotHangOnStaleCachedConnection_andRecover() throws Exception {
        Map<String, String> arangodbProps = new HashMap<>();
        arangodbProps.put("connectionCheckTimeout", String.valueOf(CHECK_TIMEOUT_MS));
        DbaasArangoDBConfigurationProperties dbaasArangoConfig = new DbaasArangoDBConfigurationProperties();
        dbaasArangoConfig.setArangodb(arangodbProps);

        TestArangoDBContainer container = TestArangoDBContainer.getInstance();

        // Positive timeout (unlike the .timeout(0) case above): this is the case the driver's own
        // idle timeout should already bound.
        ArangoDB staleDriver = new ArangoDB.Builder()
                .host("127.0.0.1", proxy.getLocalPort())
                .user(TEST_USER)
                .password(TEST_PASSWORD)
                .timeout(DRIVER_TIMEOUT_MS)
                .build();
        ArangoDB freshDriver = new ArangoDB.Builder()
                .host(container.getHost(), container.getMappedPort(DB_PORT))
                .user(TEST_USER)
                .password(TEST_PASSWORD)
                .timeout(DRIVER_TIMEOUT_MS)
                .build();

        try {
            // Warm up while the proxy is still transparent, so the connection is cached as
            // healthy before it goes silent — matching a mid-session failover, not a fresh dial.
            staleDriver.db(DB_NAME_1).query("RETURN 1", Integer.class).close();

            ArangoConnection staleConnection = new ArangoConnection();
            staleConnection.setDbName(DB_NAME_1);
            staleConnection.setArangoDatabase(staleDriver.db(DB_NAME_1));
            ArangoDatabase staleDb = new ArangoDatabase();
            staleDb.setName(DB_NAME_1);
            staleDb.setConnectionProperties(staleConnection);

            ArangoConnection freshConnection = new ArangoConnection();
            freshConnection.setDbName(DB_NAME_1);
            freshConnection.setArangoDatabase(freshDriver.db(DB_NAME_1));
            ArangoDatabase freshDb = new ArangoDatabase();
            freshDb.setName(DB_NAME_1);
            freshDb.setConnectionProperties(freshConnection);

            DatabasePool pool = mock(DatabasePool.class);
            when(pool.getOrCreateDatabase(any(), any(), any()))
                    .thenReturn(staleDb)   // first pull: the connection template warms up with
                    .thenReturn(freshDb);  // second pull: what wrapWithRetry recovers onto

            ArangoDatabaseProvider arangoDatabaseProvider = new ArangoDatabaseProvider(
                    pool,
                    new ArangoDBClassifierBuilder(null),
                    DatabaseConfig.builder().build(),
                    1, 1, CHECK_TIMEOUT_MS
            );

            GenericApplicationContext applicationContext = new GenericApplicationContext();
            applicationContext.refresh();

            DbaasArangoTemplate template = ArangoTemplateCreationUtils.getInstance()
                    .createDbaasArangoTemplate(arangoDatabaseProvider, dbaasArangoConfig, applicationContext);

            // Force the lazy connection now, while the proxy is still transparent.
            template.getArangoTemplate();

            // Now make the already-cached connection go silent, like a real failover.
            proxy.goSilent();

            try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
                Future<Integer> future = executor.submit(() -> template.query("RETURN 13", Integer.class).next());
                Integer result;
                try {
                    result = future.get(HANG_DETECTION_SECONDS, TimeUnit.SECONDS);
                } catch (TimeoutException e) {
                    future.cancel(true);
                    fail("DbaasArangoTemplate.query() hung on a stale cached connection instead of " +
                            "failing within the driver's positive timeout and recovering.");
                    return;
                }
                assertEquals(13, result,
                        "wrapWithRetry() should have recreated the template against a working " +
                                "connection and retried the query successfully.");
            }
        } finally {
            shutdownQuietly(staleDriver);
            shutdownQuietly(freshDriver);
        }
    }

    private static void shutdownQuietly(ArangoDB driver) {
        try {
            driver.shutdown();
        } catch (Exception ignored) {
            // Already shut down by DbaasArangoTemplate's own reconnect cleanup (initArangoTemplate
            // shuts down the driver it's replacing) — a double shutdown is not a test failure.
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
