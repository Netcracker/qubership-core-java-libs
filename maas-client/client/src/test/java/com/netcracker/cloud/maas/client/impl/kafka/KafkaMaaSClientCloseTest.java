package com.netcracker.cloud.maas.client.impl.kafka;

import static com.netcracker.cloud.maas.client.Utils.withProp;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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

class KafkaMaaSClientCloseTest {

    private static final String WATCH_THREAD_NAME = "watchTopicCreate";
    private static final String WATCHED_TOPIC = "orders";
    private static final String NAMESPACE = "cloud-dev";
    private static final long LONG_POLL_DELAY_MILLIS = 200;
    private static final String NO_TOPICS_YET = "[]";
    private static final String WATCHED_TOPIC_CREATED = "[{"
            + "\"name\": \"maas.core-dev.orders\","
            + "\"classifier\": {\"name\": \"" + WATCHED_TOPIC + "\", \"namespace\": \"" + NAMESPACE + "\"},"
            + "\"addresses\": {\"PLAINTEXT\": [\"localhost:9092\"]}"
            + "}]";

    private HttpServer agentStub;
    private CountDownLatch watchPolled;
    private CountDownLatch topicDelivered;
    private final AtomicBoolean topicIsCreated = new AtomicBoolean();
    private Thread watchThread;

    @BeforeEach
    void startAgentStub() throws IOException {
        watchPolled = new CountDownLatch(1);
        topicDelivered = new CountDownLatch(1);
        topicIsCreated.set(false);
        watchThread = null;

        agentStub = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        agentStub.createContext("/api-version", exchange -> respond(exchange, "{\"major\": 2, \"minor\": 8}"));
        agentStub.createContext("/api/v2/kafka/topic/watch-create", this::answerWatchPoll);
        agentStub.setExecutor(Executors.newCachedThreadPool());
        agentStub.start();
    }

    @AfterEach
    void releaseWatchThreadAndStopStub() throws InterruptedException {
        topicIsCreated.set(true);
        if (watchThread != null && watchThread.isAlive()) {
            topicDelivered.await(10, TimeUnit.SECONDS);
            watchThread.interrupt();
            watchThread.join(TimeUnit.SECONDS.toMillis(5));
        }
        agentStub.stop(0);
    }

    @Test
    void closeStopsWatchThread() {
        withProp(Env.PROP_NAMESPACE, NAMESPACE, () -> {
            String agentUrl = "http://localhost:" + agentStub.getAddress().getPort();
            withProp(Env.PROP_MAAS_AGENT_URL, agentUrl, () -> {
                Set<Thread> threadsBeforeWatch = Thread.getAllStackTraces().keySet();

                KafkaMaaSClientImpl client = createKafkaClient(agentUrl);
                client.watchTopicCreate(WATCHED_TOPIC, addr -> topicDelivered.countDown());

                assertTrue(watchPolled.await(10, TimeUnit.SECONDS),
                        "the watch thread never reached the agent stub, so the client was not left watching");
                watchThread = findWatchThreadStartedAfter(threadsBeforeWatch);
                assertNotNull(watchThread, "no new thread named '" + WATCH_THREAD_NAME + "' was started");

                client.close();

                assertFalse(watchThread.isAlive(),
                        "close() returned while '" + WATCH_THREAD_NAME + "' is still alive. "
                                + "The thread outlives the client and keeps polling " + agentUrl);
            });
        });
    }

    private static Thread findWatchThreadStartedAfter(Set<Thread> knownThreads) {
        return Thread.getAllStackTraces().keySet().stream()
                .filter(thread -> WATCH_THREAD_NAME.equals(thread.getName()))
                .filter(thread -> !knownThreads.contains(thread))
                .findFirst()
                .orElse(null);
    }

    private static KafkaMaaSClientImpl createKafkaClient(String agentUrl) {
        System.setProperty(M2MClientFactory.MAAS_AGENT_URL_PROP, agentUrl);
        var httpClient = HttpClient.getMaasClient(() -> "faketoken");
        var serverApiVersion = new ServerApiVersion(httpClient, agentUrl);
        System.clearProperty(M2MClientFactory.MAAS_AGENT_URL_PROP);

        return new KafkaMaaSClientImpl(httpClient, null, new ApiUrlProvider(serverApiVersion, agentUrl));
    }

    private void answerWatchPoll(HttpExchange exchange) throws IOException {
        watchPolled.countDown();
        if (topicIsCreated.get()) {
            respond(exchange, WATCHED_TOPIC_CREATED);
            return;
        }
        try {
            Thread.sleep(LONG_POLL_DELAY_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        respond(exchange, NO_TOPICS_YET);
    }

    private static void respond(HttpExchange exchange, String body) throws IOException {
        exchange.getRequestBody().readAllBytes();

        byte[] payload = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, payload.length);
        try (OutputStream response = exchange.getResponseBody()) {
            response.write(payload);
        }
    }
}
