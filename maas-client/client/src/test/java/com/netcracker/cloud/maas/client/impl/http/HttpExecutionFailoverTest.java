package com.netcracker.cloud.maas.client.impl.http;

import com.netcracker.cloud.maas.client.impl.Env;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.matchers.Times;
import org.mockserver.verify.VerificationTimes;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.netcracker.cloud.maas.client.Utils.withProp;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
     * so the budget is deliberately tighter than the overall duration: a wrong secret must
     * fail fast instead of hanging for the whole minute.
     */
    @Test
    void testFailover_401GivesUpAfterMaxAuthRetries(ClientAndServer mockServer) {
        mockServer.reset();
        mockServer.when(request().withPath(PATH), Times.unlimited())
                .respond(response().withStatusCode(401).withBody("{\"error\":\"unauthorized\"}"));

        withFastRetries(() ->
                assertTrue(assertThrows(RuntimeException.class,
                        () -> execution(mockServer).expect(200).sendAndReceive(String.class)
                ).getMessage().contains("401")));

        mockServer.verify(request().withPath(PATH),
                VerificationTimes.exactly(HttpExecution.MAX_AUTH_RETRIES + 1));
    }

    /**
     * A 405 without a maas-service error envelope is an ordinary "method not allowed" —
     * a route or an ingress rejecting the request — and must fail fast.
     */
    @Test
    void testFailover_405WithoutMaasEnvelopeNotRetried(ClientAndServer mockServer) {
        mockServer.reset();
        mockServer.when(request().withPath(PATH), Times.unlimited())
                .respond(response().withStatusCode(405).withBody("Method Not Allowed"));

        withFastRetries(() ->
                assertTrue(assertThrows(RuntimeException.class,
                        () -> execution(mockServer).expect(200).sendAndReceive(String.class)
                ).getMessage().contains("405")));

        mockServer.verify(request().withPath(PATH), VerificationTimes.exactly(1));
    }

    /**
     * The per-attempt budget is clamped to what is left of the total duration, so a hanging
     * agent cannot stretch the call past it.
     */
    @Test
    void testMaxTotalDuration_BoundsAHangingAttempt() throws Exception {
        // accepts the connection and never answers, unlike a refused connect which fails fast
        try (ServerSocket silentServer = new ServerSocket(0)) {
            List<Socket> accepted = Collections.synchronizedList(new ArrayList<>());
            Thread acceptor = new Thread(() -> {
                try {
                    while (!silentServer.isClosed()) {
                        accepted.add(silentServer.accept());
                    }
                } catch (IOException e) {
                    // the socket was closed, the test is over
                }
            }, "silent-server");
            acceptor.setDaemon(true);
            acceptor.start();

            try {
                withProp(Env.PROP_HTTP_RETRY_MAX_TOTAL_DURATION_MS, "1000", () -> {
                    OkHttpClient client = new OkHttpClient.Builder()
                            .readTimeout(Duration.ofMinutes(1))
                            .build();
                    Request.Builder req = new Request.Builder()
                            .url("http://127.0.0.1:" + silentServer.getLocalPort() + PATH)
                            .get();
                    HttpExecution execution = new HttpExecution(client, req).expect(200);

                    long start = System.currentTimeMillis();
                    assertThrows(RuntimeException.class, () -> execution.sendAndReceive(String.class));
                    long elapsedMs = System.currentTimeMillis() - start;
                    assertTrue(elapsedMs < 20_000,
                            "expected the call to be bounded by its 1000ms budget rather than by the "
                                    + "one minute read timeout, took " + elapsedMs + "ms");
                });
            } finally {
                synchronized (accepted) {
                    for (Socket socket : accepted) {
                        socket.close();
                    }
                }
            }
        }
    }

    @Test
    void testFailover_400NotRetried(ClientAndServer mockServer) {
        mockServer.reset();
        mockServer.when(request().withPath(PATH), Times.unlimited())
                .respond(response().withStatusCode(400).withBody("{\"error\":\"bad request\"}"));

        withFastRetries(() ->
                assertTrue(assertThrows(RuntimeException.class,
                        () -> execution(mockServer).expect(200).sendAndReceive(String.class)
                ).getMessage().contains("400")));

        mockServer.verify(request().withPath(PATH), VerificationTimes.exactly(1));
    }

    @Test
    void testInterrupt_RestoresFlagAndAbortsRetryLoop() throws Exception {
        // A long total duration keeps the retry wait long enough for the interrupt to land in it.
        withProp(Env.PROP_HTTP_RETRY_MAX_TOTAL_DURATION_MS, "600000", () -> {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(Duration.ofMillis(300))
                    .build();
            Request.Builder req = new Request.Builder().url("http://127.0.0.1:1/unreachable").get();
            HttpExecution execution = new HttpExecution(client, req);
            execution.expect(200);

            AtomicBoolean interruptedAfter = new AtomicBoolean();
            AtomicReference<Throwable> thrown = new AtomicReference<>();
            CountDownLatch started = new CountDownLatch(1);

            Thread worker = new Thread(() -> {
                started.countDown();
                try {
                    execution.sendAndReceive(String.class);
                } catch (Throwable t) {
                    thrown.set(t);
                } finally {
                    interruptedAfter.set(Thread.currentThread().isInterrupted());
                }
            }, "http-execution-interrupt-test");
            worker.start();

            assertTrue(started.await(2, TimeUnit.SECONDS));
            Thread.sleep(500);
            worker.interrupt();
            worker.join(5000);

            assertFalse(worker.isAlive(), "worker should abort instead of continuing to retry after interrupt");
            assertTrue(interruptedAfter.get(), "interrupt flag must be restored after an interrupted retry wait");
        });
    }

    // Delay must grow between attempts and saturate at a quarter of the total duration.
    @Test
    void testBackoffMillis_GrowsAndSaturatesAtTheCap() {
        long[] expectedFor60s = {1_000, 2_000, 4_000, 8_000, 15_000, 15_000};
        for (int attempt = 1; attempt <= expectedFor60s.length; attempt++) {
            assertEquals(expectedFor60s[attempt - 1], HttpExecution.cappedDelayMillis(attempt, 60_000),
                    "attempt " + attempt + " of a 60s budget");
        }

        long[] expectedFor5s = {1_000, 1_250, 1_250};
        for (int attempt = 1; attempt <= expectedFor5s.length; attempt++) {
            assertEquals(expectedFor5s[attempt - 1], HttpExecution.cappedDelayMillis(attempt, 5_000),
                    "attempt " + attempt + " of a 5s budget");
        }
    }

    // A tight max total duration must cut the retry loop short well before the attempt count is exhausted.
    @Test
    void testMaxTotalDuration_AbortsBeforeAttemptsExhausted(ClientAndServer mockServer) {
        mockServer.reset();
        mockServer.when(request().withPath(PATH), Times.unlimited())
                .respond(response().withStatusCode(500).withBody("{\"error\":\"agent down\"}"));

        withProp(Env.PROP_HTTP_RETRY_MAX_TOTAL_DURATION_MS, "200", () -> {
            long start = System.currentTimeMillis();
            assertThrows(RuntimeException.class,
                    () -> execution(mockServer).expect(200).sendAndReceive(String.class));
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
}
