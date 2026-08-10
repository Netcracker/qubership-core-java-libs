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

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.netcracker.cloud.maas.client.Utils.withProp;
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
            assertTrue(body.get().equals("ok"));
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
            assertTrue(body.get().equals("ok"));
        });

        mockServer.verify(request().withPath(PATH), VerificationTimes.exactly(3));
    }

    @Test
    void testFailover_401TwiceThenSuccess(ClientAndServer mockServer) {
        mockServer.reset();
        mockServer.when(request().withPath(PATH), Times.exactly(2))
                .respond(response().withStatusCode(401).withBody("{\"error\":\"unauthorized\"}"));
        mockServer.when(request().withPath(PATH), Times.unlimited())
                .respond(response().withStatusCode(200).withBody("\"ok\""));

        withFastRetries(() -> {
            Optional<String> body = execution(mockServer).expect(200).sendAndReceive(String.class);
            assertTrue(body.isPresent());
            assertTrue(body.get().equals("ok"));
        });

        mockServer.verify(request().withPath(PATH), VerificationTimes.exactly(3));
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

    // Delay must grow between attempts, not stay flat at the base value.
    @Test
    void testBackoffMillis_GrowsBetweenAttempts() {
        withProp(Env.PROP_HTTP_RETRY_MAX_TOTAL_DURATION_MS, "60000", () -> {
            long attempt1 = HttpExecution.cappedDelayMillis(1);
            long attempt2 = HttpExecution.cappedDelayMillis(2);
            long attempt3 = HttpExecution.cappedDelayMillis(3);
            assertTrue(attempt2 > attempt1, "expected " + attempt2 + " > " + attempt1);
            assertTrue(attempt3 > attempt2, "expected " + attempt3 + " > " + attempt2);
        });
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
        withProp(Env.PROP_HTTP_RETRY_MAX_TOTAL_DURATION_MS, "2000", test::run);
    }

    private static HttpExecution execution(ClientAndServer mockServer) {
        OkHttpClient client = new OkHttpClient();
        Request.Builder req = new Request.Builder()
                .url("http://localhost:" + mockServer.getPort() + PATH)
                .get();
        return new HttpExecution(client, req);
    }
}
