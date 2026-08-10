package com.netcracker.cloud.maas.client.impl.kafka;

import static com.netcracker.cloud.maas.client.Utils.withProp;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.netcracker.cloud.maas.client.impl.ApiUrlProvider;
import com.netcracker.cloud.maas.client.impl.Env;
import com.netcracker.cloud.maas.client.impl.apiversion.ServerApiVersion;
import com.netcracker.cloud.maas.client.impl.http.HttpClient;
import com.netcracker.cloud.security.core.utils.k8s.M2MClientFactory;
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

    /**
     * The backoff is linear at one second per consecutive failure, so this window admits the
     * first poll, a 1s pause, the second poll and a 2s pause. Anything much above that means
     * the loop is not backing off at all.
     */
    private static final long OBSERVATION_WINDOW_MILLIS = 2_500;
    private static final int MAX_EXPECTED_POLLS = 5;

    private final AtomicInteger watchPolls = new AtomicInteger();
    private final CountDownLatch firstPoll = new CountDownLatch(1);
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

    @Test
    void failingWatchPollIsBackedOffInsteadOfHotLooping() {
        withProp(Env.PROP_NAMESPACE, NAMESPACE, () -> {
            String agentUrl = "http://localhost:" + agentStub.getAddress().getPort();
            withProp(Env.PROP_MAAS_AGENT_URL, agentUrl, () -> {
                client = createKafkaClient(agentUrl);
                client.watchTopicCreate(WATCHED_TOPIC, addr -> { /* never created in this test */ });

                assertTrue(firstPoll.await(10, TimeUnit.SECONDS),
                        "the watch thread never reached the agent stub, so nothing was measured");
                Thread.sleep(OBSERVATION_WINDOW_MILLIS);

                int polls = watchPolls.get();
                // The lower bound matters as much as the upper one: without it the assertion
                // would also pass when the loop never ran and nothing was verified.
                assertTrue(polls >= 1, "watch loop did not poll at all, the test would pass vacuously");
                assertTrue(polls <= MAX_EXPECTED_POLLS,
                        "expected the watch loop to back off between failures, but it polled " + polls
                                + " times in " + OBSERVATION_WINDOW_MILLIS + "ms (limit " + MAX_EXPECTED_POLLS + ")");
            });
        });
    }

    private static KafkaMaaSClientImpl createKafkaClient(String agentUrl) {
        System.setProperty(M2MClientFactory.MAAS_AGENT_URL_PROP, agentUrl);
        var httpClient = HttpClient.getMaasClient(() -> "faketoken");
        var serverApiVersion = new ServerApiVersion(httpClient, agentUrl);
        System.clearProperty(M2MClientFactory.MAAS_AGENT_URL_PROP);

        return new KafkaMaaSClientImpl(httpClient, null, new ApiUrlProvider(serverApiVersion, agentUrl));
    }

    /** Answers every poll with 500, the code maas-agent returns when it cannot reach maas-service. */
    private void failWatchPoll(HttpExchange exchange) throws IOException {
        watchPolls.incrementAndGet();
        firstPoll.countDown();
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
