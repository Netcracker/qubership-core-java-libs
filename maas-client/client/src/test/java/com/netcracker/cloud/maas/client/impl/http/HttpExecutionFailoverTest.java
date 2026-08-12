package com.netcracker.cloud.maas.client.impl.http;

import com.netcracker.cloud.maas.client.api.MaaSHttpException;
import com.netcracker.cloud.maas.client.impl.Env;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.matchers.Times;
import org.mockserver.verify.VerificationTimes;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static com.netcracker.cloud.maas.client.Utils.withProp;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@ExtendWith(MockServerExtension.class)
class HttpExecutionFailoverTest {

    private static final String PATH = "/api/v1/kafka/topic";

    @Test
    void testFailover_405TwiceThenSuccess(ClientAndServer mockServer) {
        mockServer.reset();
        mockServer.when(request().withPath(PATH), Times.exactly(2))
                .respond(response().withStatusCode(405)
                        .withBody("{\"code\":\"MAAS-0600\",\"reason\":\"database is in read-only mode\"}"));
        mockServer.when(request().withPath(PATH), Times.unlimited())
                .respond(response().withStatusCode(200).withBody("\"ok\""));

        withFastRetries(() -> {
            Optional<String> body = execution(mockServer).expect(200).sendAndReceive(String.class);
            assertTrue(body.isPresent());
            assertEquals("ok", body.get());
        });

        mockServer.verify(request().withPath(PATH), VerificationTimes.exactly(3));
    }

    @Test
    void testFailover_500TwiceThenSuccess(ClientAndServer mockServer) {
        mockServer.reset();
        mockServer.when(request().withPath(PATH), Times.exactly(2))
                .respond(response().withStatusCode(500)
                        .withBody("{\"error\":\"error proxying request: connection refused\"}"));
        mockServer.when(request().withPath(PATH), Times.unlimited())
                .respond(response().withStatusCode(200).withBody("\"ok\""));

        withFastRetries(() -> {
            Optional<String> body = execution(mockServer).expect(200).sendAndReceive(String.class);
            assertTrue(body.isPresent());
            assertEquals("ok", body.get());
        });

        mockServer.verify(request().withPath(PATH), VerificationTimes.exactly(3));
    }

    /** An expired token clears on the next attempt, because the supplier is called again. */
    @Test
    void testFailover_401ThenSuccess(ClientAndServer mockServer) {
        mockServer.reset();
        mockServer.when(request().withPath(PATH), Times.exactly(1))
                .respond(response().withStatusCode(401).withBody("{\"error\":\"unauthorized\"}"));
        mockServer.when(request().withPath(PATH), Times.unlimited())
                .respond(response().withStatusCode(200).withBody("\"ok\""));

        withFastRetries(() -> {
            Optional<String> body = execution(mockServer).expect(200).sendAndReceive(String.class);
            assertTrue(body.isPresent());
            assertEquals("ok", body.get());
        });

        mockServer.verify(request().withPath(PATH), VerificationTimes.exactly(2));
    }

    /**
     * A 401 that keeps coming back means the supplier is handing out a token the server
     * rejects, and it has no way of being told so. Further attempts resend the same token,
     * so retrying it is deliberately capped tighter than the total duration: a wrong secret must
     * fail fast instead of hanging for the whole minute.
     */
    @Test
    void testFailover_401GivesUpAfterMaxAuthRetries(ClientAndServer mockServer) {
        mockServer.reset();
        mockServer.when(request().withPath(PATH), Times.unlimited())
                .respond(response().withStatusCode(401).withBody("{\"error\":\"unauthorized\"}"));

        withFastRetries(() -> assertMessageContains("401", execution(mockServer).expect(200)));

        mockServer.verify(request().withPath(PATH),
                VerificationTimes.exactly(HttpExecution.MAX_AUTH_RETRIES + 1));
    }

    /** Responses that are permanent, so the call must fail on its first attempt. */
    static Stream<Arguments> permanentResponses() {
        return Stream.of(
                arguments("a plain client error", 400, "{\"error\":\"bad request\"}"),
                // 405 is transient only for a read-only database; a route removed on the server
                // or an ingress rejecting the method is not
                arguments("405 without a maas-service envelope", 405, "Method Not Allowed"),
                // every maas-service error carries MAAS-0600, so the envelope alone means nothing:
                // the reason has to name the read-only database, not merely contain its words
                arguments("405 whose maas-service reason is unrelated", 405,
                        "{\"code\":\"MAAS-0600\",\"reason\":\"topic 'active-orders' is inactive\"}")
        );
    }

    @ParameterizedTest(name = "{0} is not retried")
    @MethodSource("permanentResponses")
    void testFailover_PermanentResponseNotRetried(String description, int status, String body,
                                                  ClientAndServer mockServer) {
        mockServer.reset();
        mockServer.when(request().withPath(PATH), Times.unlimited())
                .respond(response().withStatusCode(status).withBody(body));

        withFastRetries(() -> assertMessageContains(String.valueOf(status), execution(mockServer).expect(200)));

        mockServer.verify(request().withPath(PATH), VerificationTimes.exactly(1));
    }

    /**
     * Each attempt is clamped to what is left of the total duration, so a hanging
     * agent cannot stretch the call past it.
     */
    @Test
    void testMaxTotalDuration_BoundsAHangingAttempt() throws IOException {
        // accepts the connection and never answers, unlike a refused connect which fails fast
        try (ServerSocket silentServer = new ServerSocket(0)) {
            startAcceptor(silentServer, socket -> { /* hold the connection open and stay silent */ });

            withProp(Env.PROP_HTTP_RETRY_MAX_TOTAL_DURATION_MS, "1000", () -> {
                OkHttpClient client = new OkHttpClient.Builder()
                        .readTimeout(Duration.ofMinutes(1))
                        .build();
                Request.Builder req = new Request.Builder()
                        .url("http://127.0.0.1:" + silentServer.getLocalPort() + PATH)
                        .get();
                HttpExecution execution = new HttpExecution(client, req).expect(200);

                long start = System.currentTimeMillis();
                assertThrows(MaaSHttpException.class, () -> execution.sendAndReceive(String.class));
                long elapsedMs = System.currentTimeMillis() - start;
                assertTrue(elapsedMs < 5_000,
                        "expected the call to be bounded by its 1000ms total duration rather than by the "
                                + "one minute read timeout, took " + elapsedMs + "ms");
            });
        }
    }

    @Test
    void testFailover_429Retried(ClientAndServer mockServer) {
        mockServer.reset();
        mockServer.when(request().withPath(PATH), Times.exactly(1))
                .respond(response().withStatusCode(429).withBody("{\"error\":\"slow down\"}"));
        mockServer.when(request().withPath(PATH), Times.unlimited())
                .respond(response().withStatusCode(200).withBody("\"ok\""));

        withFastRetries(() -> {
            Optional<String> body = execution(mockServer).expect(200).sendAndReceive(String.class);
            assertEquals("ok", body.orElseThrow());
        });

        mockServer.verify(request().withPath(PATH), VerificationTimes.exactly(2));
    }

    /** The watch long poll owns its own loop, so its execution must send the request exactly once. */
    @Test
    void testNoRetry_SendsExactlyOneAttempt(ClientAndServer mockServer) {
        mockServer.reset();
        mockServer.when(request().withPath(PATH), Times.unlimited())
                .respond(response().withStatusCode(500).withBody("{\"error\":\"agent down\"}"));

        withFastRetries(() -> {
            HttpExecution execution = execution(mockServer).expect(200).noRetry();
            assertThrows(MaaSHttpException.class, () -> execution.sendAndReceive(String.class));
        });

        mockServer.verify(request().withPath(PATH), VerificationTimes.exactly(1));
    }

    /**
     * A zero total duration is the config-level off switch: one attempt, no retries, and no
     * per-attempt clamp that would cut that attempt short.
     */
    @Test
    void testZeroTotalDuration_SendsOneAttemptAndDoesNotRetry(ClientAndServer mockServer) {
        mockServer.reset();
        mockServer.when(request().withPath(PATH), Times.unlimited())
                .respond(response().withStatusCode(500).withBody("{\"error\":\"agent down\"}"));

        withProp(Env.PROP_HTTP_RETRY_MAX_TOTAL_DURATION_MS, "0", () -> {
            HttpExecution execution = execution(mockServer).expect(200);
            assertThrows(MaaSHttpException.class, () -> execution.sendAndReceive(String.class));
        });

        mockServer.verify(request().withPath(PATH), VerificationTimes.exactly(1));
    }

    @Test
    void testInterrupt_RestoresFlagAndAbortsRetryLoop() throws IOException {
        // A server that drops every connection: the attempt fails at once and the loop moves
        // into its backoff wait, which is where the interrupt has to land.
        try (ServerSocket rudeServer = new ServerSocket(0)) {
            CountDownLatch firstAttemptFailed = new CountDownLatch(1);
            startAcceptor(rudeServer, socket -> {
                socket.close();
                firstAttemptFailed.countDown();
            });

            // A long total duration keeps the retry wait long enough for the interrupt to land in it.
            withProp(Env.PROP_HTTP_RETRY_MAX_TOTAL_DURATION_MS, "600000", () -> {
                Request.Builder req = new Request.Builder()
                        .url("http://127.0.0.1:" + rudeServer.getLocalPort() + PATH)
                        .get();
                HttpExecution execution = new HttpExecution(new OkHttpClient(), req).expect(200);

                AtomicBoolean interruptedAfter = new AtomicBoolean();
                AtomicReference<Throwable> thrown = new AtomicReference<>();

                Thread worker = new Thread(() -> {
                    try {
                        execution.sendAndReceive(String.class);
                    } catch (Exception e) {
                        thrown.set(e);
                    } finally {
                        interruptedAfter.set(Thread.currentThread().isInterrupted());
                    }
                }, "http-execution-interrupt-test");
                worker.start();

                assertTrue(firstAttemptFailed.await(10, TimeUnit.SECONDS), "the first attempt never reached the server");
                worker.interrupt();
                worker.join(10_000);

                assertFalse(worker.isAlive(), "worker should abort instead of continuing to retry after interrupt");
                assertTrue(interruptedAfter.get(), "interrupt flag must be restored after an interrupted retry wait");
                assertInstanceOf(MaaSHttpException.class, thrown.get(),
                        "the interrupt must surface as a maas exception, not as an unrelated failure");
                assertTrue(thrown.get().getMessage().contains("Interrupted while waiting to retry"),
                        "unexpected message: " + thrown.get().getMessage());
            });
        }
    }

    /**
     * Running out of time is the usual way a failover call ends, so the exception has to say what
     * kept failing. Without the cause the trace shows only that a minute went by.
     */
    @Test
    void testTotalDurationExceeded_CarriesTheLastFailureAsCause() throws IOException {
        try (ServerSocket rudeServer = new ServerSocket(0)) {
            startAcceptor(rudeServer, Socket::close);

            withProp(Env.PROP_HTTP_RETRY_MAX_TOTAL_DURATION_MS, "1500", () -> {
                Request.Builder req = new Request.Builder()
                        .url("http://127.0.0.1:" + rudeServer.getLocalPort() + PATH)
                        .get();
                HttpExecution execution = new HttpExecution(new OkHttpClient(), req).expect(200);

                MaaSHttpException e = assertThrows(MaaSHttpException.class,
                        () -> execution.sendAndReceive(String.class));
                assertTrue(e.getMessage().contains("ran out of its"), "unexpected message: " + e.getMessage());
                assertInstanceOf(IOException.class, e.getCause(),
                        "the transport failure that consumed the time must be the cause");
            });
        }
    }

    // Delay must grow between attempts and saturate at a quarter of the total duration.
    @Test
    void testBackoffMillis_GrowsAndSaturatesAtTheCap() {
        long[] expectedFor60s = {1_000, 2_000, 4_000, 8_000, 15_000, 15_000};
        for (int attempt = 1; attempt <= expectedFor60s.length; attempt++) {
            assertEquals(expectedFor60s[attempt - 1], HttpExecution.cappedDelayMillis(attempt, 60_000),
                    "attempt " + attempt + " of a 60s total duration");
        }

        long[] expectedFor5s = {1_000, 1_250, 1_250};
        for (int attempt = 1; attempt <= expectedFor5s.length; attempt++) {
            assertEquals(expectedFor5s[attempt - 1], HttpExecution.cappedDelayMillis(attempt, 5_000),
                    "attempt " + attempt + " of a 5s total duration");
        }
    }

    // A tight max total duration must cut the retry loop short well before the attempt count is exhausted.
    @Test
    void testMaxTotalDuration_AbortsBeforeAttemptsExhausted(ClientAndServer mockServer) {
        mockServer.reset();
        mockServer.when(request().withPath(PATH), Times.unlimited())
                .respond(response().withStatusCode(500).withBody("{\"error\":\"agent down\"}"));

        withProp(Env.PROP_HTTP_RETRY_MAX_TOTAL_DURATION_MS, "200", () -> {
            HttpExecution execution = execution(mockServer).expect(200);
            long start = System.currentTimeMillis();
            assertThrows(MaaSHttpException.class, () -> execution.sendAndReceive(String.class));
            long elapsedMs = System.currentTimeMillis() - start;
            assertTrue(elapsedMs < 800,
                    "expected retry loop to abort near the 200ms max total duration, took " + elapsedMs + "ms");
        });
    }

    // A short total duration is now the only lever: it bounds both the number of attempts
    // and the pauses between them (the cap is derived as a quarter of it).
    private static void withFastRetries(Runnable test) {
        withProp(Env.PROP_HTTP_RETRY_MAX_TOTAL_DURATION_MS, "5000", test::run);
    }

    private static HttpExecution execution(ClientAndServer mockServer) {
        OkHttpClient client = new OkHttpClient();
        Request.Builder req = new Request.Builder()
                .url("http://localhost:" + mockServer.getPort() + PATH)
                .get();
        return new HttpExecution(client, req);
    }

    private static void assertMessageContains(String expected, HttpExecution execution) {
        MaaSHttpException e = assertThrows(MaaSHttpException.class, () -> execution.sendAndReceive(String.class));
        assertTrue(e.getMessage().contains(expected), "unexpected message: " + e.getMessage());
    }

    @FunctionalInterface
    private interface SocketHandler {
        void handle(Socket socket) throws IOException;
    }

    /** Serves the socket on a daemon thread until it is closed, then releases what it accepted. */
    private static void startAcceptor(ServerSocket server, SocketHandler handler) {
        Thread acceptor = new Thread(() -> {
            List<Socket> accepted = new ArrayList<>();
            try {
                while (!server.isClosed()) {
                    Socket socket = server.accept();
                    accepted.add(socket);
                    handler.handle(socket);
                }
            } catch (IOException e) {
                // the server socket was closed, the test is over
            } finally {
                accepted.forEach(HttpExecutionFailoverTest::closeQuietly);
            }
        }, "test-acceptor-" + server.getLocalPort());
        acceptor.setDaemon(true);
        acceptor.start();
    }

    private static void closeQuietly(Socket socket) {
        try {
            socket.close();
        } catch (IOException e) {
            // nothing useful to do while tearing a test down
        }
    }
}
