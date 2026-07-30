package com.netcracker.cloud.dbaas.client.arangodb.service;

import com.arangodb.ArangoDB;
import com.arangodb.springframework.core.template.ArangoTemplate;
import com.netcracker.cloud.dbaas.client.arangodb.classifier.ArangoDBClassifierBuilder;
import com.netcracker.cloud.dbaas.client.arangodb.configuration.DbaasArangoDBConfigurationProperties;
import com.netcracker.cloud.dbaas.client.arangodb.entity.connection.ArangoConnection;
import com.netcracker.cloud.dbaas.client.arangodb.entity.database.ArangoDatabase;
import com.netcracker.cloud.dbaas.client.management.ArangoDatabaseProvider;
import com.netcracker.cloud.dbaas.client.management.DatabaseConfig;
import com.netcracker.cloud.dbaas.client.management.DatabasePool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Reproduces the deadlock: when an ArangoDB cluster leader is replaced and the old TCP
 * endpoint is stale, operations block forever because arangodb.timeout defaults to 0 (infinite).
 * <p>
 * The black-hole ServerSocket accepts TCP connections, drains incoming bytes, but never
 * writes a response — mimicking a dead node after failover. Draining is required to keep
 * the OS send-buffer from filling; without it the driver fails with a write error before
 * the timeout fires, masking the problem.
 * <p>
 * Tests FAIL with no default timeout and PASS once a positive default timeout.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = CheckConnectionTimeoutTest.Config.class)
class CheckConnectionTimeoutTest {

    @Configuration
    @EnableConfigurationProperties(DbaasArangoDBConfigurationProperties.class)
    static class Config {
    }

    private static final int HANG_DETECTION_SECONDS = 10;
    private static final String DB_NAME = "nonexistent";
    // Independent of props.checkConnectionTimeoutMs() (now decoupled from dbaas.arangodb.timeout)
    // so these provider tests stay fast regardless of the production default.
    private static final long PROVIDER_CHECK_TIMEOUT_MS = 100;

    @Autowired
    private DbaasArangoDBConfigurationProperties props;

    private ServerSocket blackHoleServer;

    @BeforeEach
    void beforeEach() throws IOException {
        Optional<Integer> configuredTimeout = props.asArangoConfigProperties().getTimeout();
        if (configuredTimeout.isPresent() && configuredTimeout.get() > 0) {
            props.getArangodb().put("timeout", "100"); // speed up tests when a default is configured
        }
        startBlackHoleServer();
    }

    @AfterEach
    void afterEach() throws IOException {
        blackHoleServer.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    void dbaasArangoTemplate_query_shouldNotHang() throws Exception {
        com.arangodb.ArangoDatabase realDb = buildBlackHoleDriver().db(DB_NAME);

        ArangoTemplate hangingOps = mock(ArangoTemplate.class);
        when(hangingOps.query(any(String.class), any(Class.class)))
                .thenAnswer(inv -> realDb.query(inv.getArgument(0), Integer.class));

        DbaasArangoTemplate template = new DbaasArangoTemplate(null, null, null, props, null) {
            @Override
            protected ArangoTemplate getArangoTemplate() {
                return hangingOps;
            }

            @Override
            protected void initArangoTemplate() {
                throw new UnsupportedOperationException("not needed");
            }
        };

        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<?> future = executor.submit(() -> template.query("RETURN 13", Integer.class));
            assertNotHang(future,
                    "DbaasArangoTemplate.query() blocked indefinitely against a non-responding server. " +
                            "Ensure dbaas.arangodb.timeout is set to a positive value.");
        }
    }

    @Test
    @SuppressWarnings("java:S1612") // method ref is ambiguous here, see comment below
    void arangoDatabaseProvider_provide_shouldNotHang() throws Exception {
        ArangoDB driver = buildBlackHoleDriver();
        ArangoConnection connection = new ArangoConnection();
        connection.setHost("127.0.0.1");
        connection.setPort(blackHoleServer.getLocalPort());
        connection.setDbName(DB_NAME);
        connection.setArangoDatabase(driver.db(DB_NAME));

        ArangoDatabase arangoDatabase = new ArangoDatabase();
        arangoDatabase.setName(DB_NAME);
        arangoDatabase.setConnectionProperties(connection);

        DatabasePool pool = mock(DatabasePool.class);
        when(pool.getOrCreateDatabase(any(), any(), any())).thenReturn(arangoDatabase);

        // retries=1, retryDelay=1: small but non-zero so a real retry happens without waiting
        // on the production retry delay.
        ArangoDatabaseProvider provider = new ArangoDatabaseProvider(
                pool,
                new ArangoDBClassifierBuilder(null),
                DatabaseConfig.builder().build(),
                1, 1, PROVIDER_CHECK_TIMEOUT_MS
        );

        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            // Not a method reference: ExecutorService.submit() is overloaded for Callable<T> and
            // Runnable, and provider::provide (non-void) is ambiguous between the two overloads.
            Future<?> future = executor.submit(() -> provider.provide());
            assertNotHang(future,
                    "ArangoDatabaseProvider.provide() blocked indefinitely against a non-responding server. " +
                            "Ensure dbaas.arangodb.timeout is set to a positive value.");
        }
    }

    @Test
    void arangoDatabaseProvider_provide_shouldThrowWhenAllChecksFail() {
        ArangoDB driver = buildBlackHoleDriver();
        ArangoConnection connection = new ArangoConnection();
        connection.setHost("127.0.0.1");
        connection.setPort(blackHoleServer.getLocalPort());
        connection.setDbName(DB_NAME);
        connection.setArangoDatabase(driver.db(DB_NAME));

        ArangoDatabase arangoDatabase = new ArangoDatabase();
        arangoDatabase.setName(DB_NAME);
        arangoDatabase.setConnectionProperties(connection);

        DatabasePool pool = mock(DatabasePool.class);
        when(pool.getOrCreateDatabase(any(), any(), any())).thenReturn(arangoDatabase);

        // retries=1, retryDelay=1: small but non-zero so a real retry happens without waiting
        // on the production retry delay.
        ArangoDatabaseProvider provider = new ArangoDatabaseProvider(
                pool,
                new ArangoDBClassifierBuilder(null),
                DatabaseConfig.builder().build(),
                1, 1, PROVIDER_CHECK_TIMEOUT_MS
        );

        // the check always times out against the black hole -> retries exhausted -> throw
        assertThrows(IllegalStateException.class, provider::provide);
    }

    private ArangoDB buildBlackHoleDriver() {
        return new ArangoDB.Builder()
                .host("127.0.0.1", blackHoleServer.getLocalPort())
                .loadProperties(props.asArangoConfigProperties())
                .build();
    }

    private void startBlackHoleServer() throws IOException {
        blackHoleServer = new ServerSocket(0);

        Thread acceptThread = new Thread(() -> {
            while (!blackHoleServer.isClosed()) {
                try {
                    Socket client = blackHoleServer.accept();
                    // Drain incoming bytes so the OS send-buffer never fills and the driver
                    // stays blocked waiting for an HTTP response rather than failing on a
                    // write error. Never send a response.
                    Thread drainer = new Thread(() -> {
                        try {
                            client.getInputStream().transferTo(OutputStream.nullOutputStream());
                        } catch (IOException ignored) {
                            // Exception handling is not relevant to the purpose of this thread
                        }
                    }, "black-hole-drainer");
                    drainer.setDaemon(true);
                    drainer.start();
                } catch (IOException ignored) {
                    // SocketException on close() — normal shutdown
                }
            }
        }, "black-hole-acceptor");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    private void assertNotHang(Future<?> future, String failMessage)
            throws InterruptedException {
        try {
            future.get(HANG_DETECTION_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            fail(failMessage);
        } catch (ExecutionException e) {
            // Completed with an exception — did not hang. Expected when a positive
            // timeout is configured and fires.
        }
    }
}
