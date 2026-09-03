package com.netcracker.cloud.maas.client.impl.kafka;

import static com.netcracker.cloud.maas.client.Utils.withProp;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.Duration;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.netcracker.cloud.maas.client.impl.ApiUrlProvider;
import com.netcracker.cloud.maas.client.impl.Env;
import com.netcracker.cloud.maas.client.impl.apiversion.ServerApiVersion;
import com.netcracker.cloud.maas.client.impl.http.HttpClient;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Pins the invariant that a failing {@code watch-create} long poll is retried with a backoff
 * rather than in a hot loop.
 *
 * <p>Without one, a maas-agent that is down gets polled as fast as the socket can refuse the
 * connection — hammering it exactly while it is coming back up.
 */
class KafkaMaaSClientWatchBackoffTest {

    private static final String WATCHED_TOPIC = "orders";
    private static final String NAMESPACE = "cloud-dev";

    /** Three polls are enough to see the pause between them grow. */
    private static final int OBSERVED_POLLS = 3;

    private final List<Long> pollMillis = Collections.synchronizedList(new ArrayList<>());
    private final CountDownLatch pollsObserved = new CountDownLatch(OBSERVED_POLLS);
    private HttpServer agentStub;
    private KafkaMaaSClientImpl client;

    @BeforeEach
    void startAgentStub() throws IOException {
        agentStub = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        agentStub.createContext("/api-version", exchange -> respond(exchange, 200, "{\"major\": 2, \"minor\": 8}"));
        agentStub.createContext("/api/v2/kafka/topic/watch-create", this::failWatchPoll);
        agentStub.start();
    }

    @AfterEach
    void stopClientAndStub() {
        if (client != null) {
            client.close();
        }
        agentStub.stop(0);
    }

    /**
     * maas-service holds a watch poll open for the whole requested window and then answers 200
     * with an empty list. If the window outlasts the client read timeout, that answer never
     * arrives: every quiet poll fails locally, walks the backoff up to its 30s cap and delays
     * the next real topic-create event.
     */
    @Test
    void watchWindowStaysBelowTheReadTimeout() {
        for (long readTimeoutSeconds : new long[]{2, 5, 6, 10, 30, 60, 120}) {
            Duration readTimeout = Duration.ofSeconds(readTimeoutSeconds);
            Duration window = KafkaMaaSClientImpl.watchTimeout(readTimeout);
            assertTrue(window.compareTo(readTimeout) < 0,
                    "a " + readTimeoutSeconds + "s read timeout must leave room for the answer, got " + window);
            assertFalse(window.isZero() || window.isNegative(),
                    "the window must stay positive, got " + window);
        }
        assertEquals(Duration.ofSeconds(25), KafkaMaaSClientImpl.watchTimeout(Duration.ofSeconds(30)),
                "the default read timeout should keep the full margin");
    }

    @Test
    void backoffGrowsWithConsecutiveFailures() {
        assertEquals(1_000, KafkaMaaSClientImpl.watchBackoffMillis(1));
        assertEquals(2_000, KafkaMaaSClientImpl.watchBackoffMillis(2));
        assertEquals(3_000, KafkaMaaSClientImpl.watchBackoffMillis(3));
        assertEquals(30_000, KafkaMaaSClientImpl.watchBackoffMillis(30), "capped");
        assertEquals(30_000, KafkaMaaSClientImpl.watchBackoffMillis(1_000), "stays at the cap");
    }

    @Test
    void failingWatchPollIsBackedOffInsteadOfHotLooping() {
        withProp(Env.PROP_NAMESPACE, NAMESPACE, () -> {
            String agentUrl = "http://localhost:" + agentStub.getAddress().getPort();
            withProp(Env.PROP_MAAS_AGENT_URL, agentUrl, () -> {
                client = createKafkaClient(agentUrl);
                client.watchTopicCreate(WATCHED_TOPIC, addr -> { /* never created in this test */ });

                assertTrue(pollsObserved.await(30, TimeUnit.SECONDS),
                        "the watch loop reached the agent stub only " + pollMillis.size()
                                + " times out of " + OBSERVED_POLLS + ", so nothing was measured");

                for (int poll = 1; poll < OBSERVED_POLLS; poll++) {
                    long pause = pollMillis.get(poll) - pollMillis.get(poll - 1);
                    assertTrue(pause > 500,
                            "expected the watch loop to pause after a failure, but poll " + poll
                                    + " followed the previous one in " + pause + "ms");
                }
            });
        });
    }

    private static KafkaMaaSClientImpl createKafkaClient(String agentUrl) {
        var httpClient = HttpClient.getMaasClient(() -> "faketoken");
        var serverApiVersion = new ServerApiVersion(httpClient, agentUrl);

        return new KafkaMaaSClientImpl(httpClient,
                () -> { throw new UnsupportedOperationException("tenant manager is not used in this test"); },
                new ApiUrlProvider(serverApiVersion, agentUrl));
    }

    /** Answers every poll with 500, the code maas-agent returns when it cannot reach maas-service. */
    private void failWatchPoll(HttpExchange exchange) throws IOException {
        pollMillis.add(System.currentTimeMillis());
        pollsObserved.countDown();
        respond(exchange, 500, "{\"error\":\"error proxying request: maas-service unavailable\"}");
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        exchange.getRequestBody().readAllBytes();

        byte[] payload = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, payload.length);
        try (OutputStream response = exchange.getResponseBody()) {
            response.write(payload);
        }
    }
}
