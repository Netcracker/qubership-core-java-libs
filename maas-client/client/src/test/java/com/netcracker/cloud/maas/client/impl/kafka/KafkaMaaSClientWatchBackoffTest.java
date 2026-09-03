package com.netcracker.cloud.maas.client.impl.kafka;

import static com.netcracker.cloud.maas.client.Utils.withProp;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
        // the invariant, checked across the range rather than at one point
        for (long readTimeoutSeconds : new long[]{2, 5, 6, 10, 30, 60, 120}) {
            Duration window = KafkaMaaSClientImpl.watchTimeout(Duration.ofSeconds(readTimeoutSeconds));
            assertTrue(window.getSeconds() < readTimeoutSeconds,
                    "a " + readTimeoutSeconds + "s read timeout must leave room for the answer, got " + window);
            assertTrue(window.getSeconds() >= 1,
                    "the window travels in whole seconds, so it must not round down to zero: " + window);
        }
        assertEquals(Duration.ofSeconds(25), KafkaMaaSClientImpl.watchTimeout(Duration.ofSeconds(30)),
                "the default read timeout should keep the full margin");
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

                long firstPause = pollMillis.get(1) - pollMillis.get(0);
                long secondPause = pollMillis.get(2) - pollMillis.get(1);
                // A hot loop would show pauses near zero; a fixed delay would show two equal ones.
                assertTrue(firstPause > 500,
                        "expected the watch loop to pause after a failure, but it polled again in " + firstPause + "ms");
                assertTrue(secondPause > firstPause,
                        "expected the pause to grow with consecutive failures, but got "
                                + firstPause + "ms then " + secondPause + "ms");
            });
        });
    }

    private static KafkaMaaSClientImpl createKafkaClient(String agentUrl) {
        System.setProperty(Env.PROP_MAAS_AGENT_URL, agentUrl);
        var httpClient = HttpClient.getMaasClient(() -> "faketoken");
        var serverApiVersion = new ServerApiVersion(httpClient, agentUrl);
        System.clearProperty(Env.PROP_MAAS_AGENT_URL);

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
