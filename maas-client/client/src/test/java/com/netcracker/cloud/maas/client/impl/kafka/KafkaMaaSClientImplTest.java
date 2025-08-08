package org.qubership.cloud.maas.client.impl.kafka;

import static org.qubership.cloud.maas.client.Utils.readResourceAsString;
import static org.qubership.cloud.maas.client.Utils.withProp;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.JsonBody.json;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.matchers.MatchType;
import org.mockserver.matchers.Times;
import org.mockserver.mock.action.ExpectationResponseCallback;
import org.mockserver.model.HttpRequest;

import org.qubership.cloud.maas.client.Utils;
import org.qubership.cloud.maas.client.api.Classifier;
import org.qubership.cloud.maas.client.api.MaaSException;
import org.qubership.cloud.maas.client.api.kafka.KafkaMaaSClient;
import org.qubership.cloud.maas.client.api.kafka.SearchCriteria;
import org.qubership.cloud.maas.client.api.kafka.TopicAddress;
import org.qubership.cloud.maas.client.api.kafka.TopicCreateOptions;
import org.qubership.cloud.maas.client.api.kafka.protocolextractors.OnTopicExists;
import org.qubership.cloud.maas.client.impl.ApiUrlProvider;
import org.qubership.cloud.maas.client.impl.Env;
import org.qubership.cloud.maas.client.impl.MaaSAPIClientImpl;
import org.qubership.cloud.maas.client.impl.apiversion.ServerApiVersion;
import org.qubership.cloud.maas.client.impl.dto.kafka.v1.TopicTemplate;
import org.qubership.cloud.maas.client.impl.http.HttpClient;

import lombok.extern.slf4j.Slf4j;

@ExtendWith(MockServerExtension.class)
@Slf4j
// TODO tests with kafka SSL+password
class KafkaMaaSClientImplTest {
    @BeforeEach
    public void setup(ClientAndServer mockServer) {
        mockServer.reset();
        mockServer.when(
                request()
                        .withPath("/api-version")
        ).respond(
                response().withBody("{\"major\":2, \"minor\": 16}")
        );
    }

    @Test
    public void testGetNonExisingTenantTopic(ClientAndServer mockServer) {
        // prepare environment
        withProp(Env.PROP_NAMESPACE, "core-dev", () -> {
            mockServer.when(
                    request()
                            .withPath("/api/v2/kafka/topic/get-by-classifier")
                            .withHeader("authorization", "Bearer faketoken")
                            .withBody(
                                    json("{" +
                                                    "    \"name\": \"orders\"," +
                                                    "    \"namespace\": \"core-dev\"," +
                                                    "    \"tenantId\": \"d047619f-6886-4842-81a7-3f87cb748ac1\"" +
                                                    "}",
                                            MatchType.STRICT
                                    )),
                    Times.once()
            ).respond(
                    response().withStatusCode(404)
            );

            // run test
            KafkaMaaSClientImpl client = createKafkaClient("http://localhost:" + mockServer.getPort());
            Optional<TopicAddress> topicAddress = client.getTopic(new Classifier("orders").tenantId("d047619f-6886-4842-81a7-3f87cb748ac1"));
            assertFalse(topicAddress.isPresent());
        });
    }

    @Test
    public void testGetTenantTopic(ClientAndServer mockServer) {
        // prepare environment
        withProp(Env.PROP_NAMESPACE, "core-dev", () -> {
            mockServer.when(
                    request()
                            .withPath("/api/v2/kafka/topic/get-by-classifier")
                            .withHeader("authorization", "Bearer faketoken")
                            .withBody(
                                    json("{" +
                                                    "    \"name\": \"orders\"," +
                                                    "    \"namespace\": \"core-dev\"," +
                                                    "    \"tenantId\": \"d047619f-6886-4842-81a7-3f87cb748ac1\"" +
                                                    "}",
                                            MatchType.STRICT
                                    )),
                    Times.once() // also test cache feature, so call should be executed only once
            ).respond(
                    response()
                            .withStatusCode(200)
                            .withBody("{\n" +
                                    "  \"addresses\": {\n" +
                                    "      \"PLAINTEXT\": [\n" +
                                    "          \"my-kafka.kafka-cluster:9092\"\n" +
                                    "       ]\n" +
                                    "  }, \n" +
                                    "  \"name\": \"maas.core_dev.orders.1234567\",\n" +
                                    "  \"classifier\": {\n" +
                                    "    \"name\": \"orders\",\n" +
                                    "    \"namespace\": \"core-dev\",\n" +
                                    "    \"tenantId\": \"d047619f-6886-4842-81a7-3f87cb748ac1\"\n" +
                                    "  }, \n" +
                                    "  \"namespace\": \"core-dev\",\n" +
                                    "  \"instance\": \"default\",\n" +
                                    "  \"requestedSettings\": {\n" +
                                    "    \"numPartitions\": 1,\n" +
                                    "    \"replicationFactor\": 1,\n" +
                                    "    \"replicaAssignment\": null,\n" +
                                    "    \"configs\": null\n" +
                                    "  },\n" +
                                    "  \"actualSettings\": {\n" +
                                    "    \"numPartitions\": 1,\n" +
                                    "    \"replicationFactor\": 1,\n" +
                                    "    \"replicaAssignment\": {\n" +
                                    "      \"0\": [ 0 ]\n" +
                                    "    },\n" +
                                    "    \"configs\": {\n" +
                                    "      \"cleanup.policy\": \"delete\"\n" +
                                    "      }\n" +
                                    "    } \n" +
                                    "}\n")
            );

            // run test
            KafkaMaaSClientImpl client = createKafkaClient("http://localhost:" + mockServer.getPort());
            TopicAddress topicAddress = client.getTopic(new Classifier("orders").tenantId("d047619f-6886-4842-81a7-3f87cb748ac1")).orElseGet(() -> fail());
            assertEquals("maas.core_dev.orders.1234567", topicAddress.getTopicName());
            assertEquals("my-kafka.kafka-cluster:9092", topicAddress.getBoostrapServers("PLAINTEXT"));
        });
    }

    @Test
    public void testGetCompositeTopic(ClientAndServer mockServer) {
        // prepare environment
        withProp(Env.PROP_NAMESPACE, "core-dev", () -> {
            mockServer.when(
                    request()
                            .withPath("/api/v2/kafka/topic/get-by-classifier")
                            .withHeader("authorization", "Bearer faketoken")
                            .withBody(
                                    json("{" +
                                                    "    \"name\": \"orders\"," +
                                                    "    \"namespace\": \"friendly-ns\"" +
                                                    "}",
                                            MatchType.STRICT
                                    )),
                    Times.once() // also test cache feature, so call should be executed only once
            ).respond(
                    response()
                            .withStatusCode(200)
                            .withBody("{\n" +
                                    "  \"addresses\": {\n" +
                                    "      \"PLAINTEXT\": [\n" +
                                    "          \"my-kafka.kafka-cluster:9092\"\n" +
                                    "       ]\n" +
                                    "  }, \n" +
                                    "  \"name\": \"maas.core_dev.orders.1234567\",\n" +
                                    "  \"classifier\": {\n" +
                                    "    \"name\": \"orders\",\n" +
                                    "    \"namespace\": \"friendly-ns\",\n" +
                                    "    \"tenantId\": \"d047619f-6886-4842-81a7-3f87cb748ac1\"\n" +
                                    "  }, \n" +
                                    "  \"namespace\": \"core-dev\",\n" +
                                    "  \"instance\": \"default\",\n" +
                                    "  \"requestedSettings\": {\n" +
                                    "    \"numPartitions\": 1,\n" +
                                    "    \"replicationFactor\": 1,\n" +
                                    "    \"replicaAssignment\": null,\n" +
                                    "    \"configs\": null\n" +
                                    "  },\n" +
                                    "  \"actualSettings\": {\n" +
                                    "    \"numPartitions\": 1,\n" +
                                    "    \"replicationFactor\": 1,\n" +
                                    "    \"replicaAssignment\": {\n" +
                                    "      \"0\": [ 0 ]\n" +
                                    "    },\n" +
                                    "    \"configs\": {\n" +
                                    "      \"cleanup.policy\": \"delete\"\n" +
                                    "      }\n" +
                                    "    } \n" +
                                    "}\n")
            );

            // run test
            KafkaMaaSClientImpl client = createKafkaClient("http://localhost:" + mockServer.getPort());
            TopicAddress topicAddress = client.getTopic(new Classifier("orders").namespace("friendly-ns")).orElseGet(() -> fail());
            assertEquals("maas.core_dev.orders.1234567", topicAddress.getTopicName());
            assertEquals("my-kafka.kafka-cluster:9092", topicAddress.getBoostrapServers("PLAINTEXT"));
        });
    }

    @Test
    public void testNoNullValuesInCache(ClientAndServer mockServer) {
        // prepare environment
        withProp(Env.PROP_NAMESPACE, "core-dev", () -> {
            mockServer.when(
                    request()
                            .withPath("/api/v2/kafka/topic/get-by-classifier")
                            .withHeader("authorization", "Bearer faketoken")
                            .withBody(
                                    json("{" +
                                                    "    \"name\": \"orders\"," +
                                                    "    \"namespace\": \"core-dev\"," +
                                                    "    \"tenantId\": \"d047619f-6886-4842-81a7-3f87cb748ac1\"" +
                                                    "}",
                                            MatchType.STRICT
                                    )),
                    Times.once()
            ).respond(
                    response().withStatusCode(404)
            );

            // run test
            KafkaMaaSClientImpl client = createKafkaClient("http://localhost:" + mockServer.getPort());
            Optional<TopicAddress> topicAddress = client.getTopic(new Classifier("orders").tenantId("d047619f-6886-4842-81a7-3f87cb748ac1"));
            assertFalse(topicAddress.isPresent());

            mockServer.when(
                    request()
                            .withPath("/api/v2/kafka/topic/get-by-classifier")
                            .withHeader("authorization", "Bearer faketoken")
                            .withBody(
                                    json("{" +
                                                    "    \"name\": \"orders\"," +
                                                    "    \"namespace\": \"core-dev\"," +
                                                    "    \"tenantId\": \"d047619f-6886-4842-81a7-3f87cb748ac1\"" +
                                                    "}",
                                            MatchType.STRICT
                                    )),
                    Times.once()
            ).respond(
                    response()
                            .withStatusCode(200)
                            .withBody("{\n" +
                                    "  \"addresses\": {\n" +
                                    "      \"PLAINTEXT\": [\n" +
                                    "          \"my-kafka.kafka-cluster:9092\"\n" +
                                    "       ]\n" +
                                    "  }, \n" +
                                    "  \"name\": \"maas.core_dev.orders.1234567\",\n" +
                                    "  \"classifier\": {\n" +
                                    "    \"name\": \"orders\",\n" +
                                    "    \"namespace\": \"core-dev\",\n" +
                                    "    \"tenantId\": \"d047619f-6886-4842-81a7-3f87cb748ac1\"\n" +
                                    "  }, \n" +
                                    "  \"namespace\": \"core-dev\",\n" +
                                    "  \"instance\": \"default\",\n" +
                                    "  \"requestedSettings\": {\n" +
                                    "    \"numPartitions\": 1,\n" +
                                    "    \"replicationFactor\": 1,\n" +
                                    "    \"replicaAssignment\": null,\n" +
                                    "    \"configs\": null\n" +
                                    "  },\n" +
                                    "  \"actualSettings\": {\n" +
                                    "    \"numPartitions\": 1,\n" +
                                    "    \"replicationFactor\": 1,\n" +
                                    "    \"replicaAssignment\": {\n" +
                                    "      \"0\": [ 0 ]\n" +
                                    "    },\n" +
                                    "    \"configs\": {\n" +
                                    "      \"cleanup.policy\": \"delete\"\n" +
                                    "      }\n" +
                                    "    } \n" +
                                    "}\n")
            );

            topicAddress = client.getTopic(new Classifier("orders").tenantId("d047619f-6886-4842-81a7-3f87cb748ac1"));
            assertTrue(topicAddress.isPresent());
        });
    }

    @Test
    public void testGetOrCreateLazyTenantTopic(ClientAndServer mockServer) {
        // prepare environment
        withProp(Env.PROP_NAMESPACE, "core-dev", () -> {
            mockServer.when(
                    request()
                            .withPath("/api/v2/kafka/lazy-topic")
                            .withHeader("authorization", "Bearer faketoken")
                            .withBody(
                                    json("{" +
                                                    "    \"name\": \"orders\"," +
                                                    "    \"namespace\": \"core-dev\"," +
                                                    "    \"tenantId\": \"d047619f-6886-4842-81a7-3f87cb748ac1\"" +
                                                    "}",
                                            MatchType.STRICT
                                    )),
                    Times.once()
            ).respond(
                    response()
                            .withStatusCode(200)
                            .withBody("{\n" +
                                    "  \"addresses\": {\n" +
                                    "      \"PLAINTEXT\": [\n" +
                                    "          \"my-kafka.kafka-cluster:9092\"\n" +
                                    "       ]\n" +
                                    "  }, \n" +
                                    "  \"name\": \"maas.core_dev.orders.1234567\",\n" +
                                    "  \"classifier\": {\n" +
                                    "    \"name\": \"orders\",\n" +
                                    "    \"namespace\": \"core-dev\",\n" +
                                    "    \"tenantId\": \"d047619f-6886-4842-81a7-3f87cb748ac1\"\n" +
                                    "  }, \n" +
                                    "  \"namespace\": \"core-dev\",\n" +
                                    "  \"instance\": \"default\",\n" +
                                    "  \"requestedSettings\": {\n" +
                                    "    \"numPartitions\": 1,\n" +
                                    "    \"replicationFactor\": 1,\n" +
                                    "    \"replicaAssignment\": null,\n" +
                                    "    \"configs\": null\n" +
                                    "  },\n" +
                                    "  \"actualSettings\": {\n" +
                                    "    \"numPartitions\": 1,\n" +
                                    "    \"replicationFactor\": 1,\n" +
                                    "    \"replicaAssignment\": {\n" +
                                    "      \"0\": [ 0 ]\n" +
                                    "    },\n" +
                                    "    \"configs\": {\n" +
                                    "      \"cleanup.policy\": \"delete\"\n" +
                                    "      }\n" +
                                    "    } \n" +
                                    "}\n")
            );

            // run test
            KafkaMaaSClientImpl client = createKafkaClient("http://localhost:" + mockServer.getPort());
            TopicAddress topicAddress = client.getOrCreateLazyTopic("orders", "d047619f-6886-4842-81a7-3f87cb748ac1");
            assertNotNull(topicAddress);
            assertEquals("maas.core_dev.orders.1234567", topicAddress.getTopicName());
            assertEquals("my-kafka.kafka-cluster:9092", topicAddress.getBoostrapServers("PLAINTEXT"));
        });
    }

    @Test
    public void testGetOrCreateTopic(ClientAndServer mockServer) {
        // prepare environment
        withProp(Env.PROP_NAMESPACE, "cloudbss-kube-core-demo-2", () -> {
            mockServer.when(
                    request()
                            .withPath("/api/v2/kafka/topic")
                            .withQueryStringParameter("onTopicExists", "merge")
                            .withHeader("authorization", "Bearer faketoken")
                            .withBody(
                                    json("{\n" +
                                                    "      \"classifier\" : {\n" +
                                                    "        \"name\" : \"orders\",\n" +
                                                    "        \"namespace\" : \"cloudbss-kube-core-demo-2\"\n" +
                                                    "      },\n" +
                                                    "      \"name\" : \"user-test1\",\n" +
                                                    "      \"template\" : \"base\",\n" +
                                                    "      \"configs\" : {\n" +
                                                    "        \"compression.type\" : \"snappy\"\n" +
                                                    "      }\n" +
                                                    "    }",
                                            MatchType.STRICT
                                    )),
                    Times.once()
            ).respond(
                    response()
                            .withStatusCode(200)
                            .withBody("{\n" +
                                    "  \"addresses\": {\n" +
                                    "      \"PLAINTEXT\": [\n" +
                                    "          \"my-kafka.kafka-cluster:9092\"\n" +
                                    "       ]\n" +
                                    "  }, \n" +
                                    "  \"name\": \"user-test1\",\n" +
                                    "  \"classifier\": {\n" +
                                    "    \"name\": \"orders\",\n" +
                                    "    \"namespace\": \"cloudbss-kube-core-demo-2\",\n" +
                                    "    \"tenantId\": \"d047619f-6886-4842-81a7-3f87cb748ac1\"\n" +
                                    "  }, \n" +
                                    "  \"namespace\": \"cloudbss-kube-core-demo-2\",\n" +
                                    "  \"versioned\": \"false\",\n" +
                                    "  \"instance\": \"default\",\n" +
                                    "  \"requestedSettings\": {\n" +
                                    "    \"numPartitions\": 1,\n" +
                                    "    \"replicationFactor\": 1,\n" +
                                    "    \"replicaAssignment\": null,\n" +
                                    "    \"configs\": null\n" +
                                    "  },\n" +
                                    "  \"actualSettings\": {\n" +
                                    "    \"numPartitions\": 1,\n" +
                                    "    \"replicationFactor\": 1,\n" +
                                    "    \"replicaAssignment\": {\n" +
                                    "      \"0\": [ 0 ]\n" +
                                    "    },\n" +
                                    "    \"configs\": {\n" +
                                    "       \"compression.type\" : \"snappy\"\n" +
                                    "      }\n" +
                                    "    }, \n" +
                                    "    \"template\": \"base\"" +
                                    "}\n")
            );

            // run test
            var client = createKafkaClient("http://localhost:" + mockServer.getPort());
            TopicAddress topicAddress = client.getOrCreateTopic(new Classifier("orders"),
                    TopicCreateOptions.builder()
                            .onTopicExists(OnTopicExists.MERGE)
                            .name("user-test1")
                            .config("compression.type", "snappy")
                            .template("base")
                            .build());
            assertNotNull(topicAddress);
            assertEquals("user-test1", topicAddress.getTopicName());
            assertEquals("my-kafka.kafka-cluster:9092", topicAddress.getBoostrapServers("PLAINTEXT"));
        });
    }

    @Test
    public void testGetOrCreateTopicWithRetry(ClientAndServer mockServer) throws IOException {
        // prepare environment
        withProp(Env.PROP_NAMESPACE, "cloudbss-kube-core-demo-2", () -> {
            withProp(Env.PROP_HTTP_TIMEOUT, "1", () -> {

                mockServer.when(
                        request()
                                .withPath("/api/v2/kafka/topic"),
                        Times.once()
                ).respond(
                        response()
                                .withStatusCode(200)
                                .withDelay(TimeUnit.SECONDS, 2)
                );

                mockServer.when(
                        request()
                                .withPath("/api/v2/kafka/topic"),
                        Times.once()
                ).respond(
                        response()
                                .withStatusCode(200)
                                .withBody("{\n" +
                                        "  \"name\": \"user-test1\"\n" +
                                        "}\n")
                );

                // run test
                var client = createKafkaClient("http://localhost:" + mockServer.getPort());
                TopicAddress topicAddress = client.getOrCreateTopic(new Classifier("orders"),
                        TopicCreateOptions.builder()
                                .name("user-test1")
                                .build());
                assertEquals("user-test1", topicAddress.getTopicName());
            });
        });
    }

    @Test
    public void testGetOrCreateTopicV2(ClientAndServer mockServer) {
        // prepare environment
        withProp(Env.PROP_NAMESPACE, "cloudbss-kube-core-demo-2", () -> {
            mockServer.when(
                    request().withPath("/api-version"),
                    Times.once()
            ).respond(
                    response().withBody("{\"major\":2, \"minor\": 8}")
            );
            mockServer.when(
                    request()
                            .withPath("/api/v2/kafka/topic")
                            .withQueryStringParameter("onTopicExists", "merge")
                            .withHeader("authorization", "Bearer faketoken")
                            .withBody(
                                    json("{\n" +
                                                    "      \"classifier\" : {\n" +
                                                    "        \"name\" : \"orders\",\n" +
                                                    "        \"namespace\" : \"cloudbss-kube-core-demo-2\"\n" +
                                                    "      },\n" +
                                                    "      \"name\" : \"user-test1\",\n" +
                                                    "      \"minNumPartitions\" : 2\n" +
                                                    "    }",
                                            MatchType.STRICT
                                    )),
                    Times.once()
            ).respond(
                    response()
                            .withStatusCode(200)
                            .withBody("{\n" +
                                    "  \"addresses\": {\n" +
                                    "      \"PLAINTEXT\": [\n" +
                                    "          \"my-kafka.kafka-cluster:9092\"\n" +
                                    "       ]\n" +
                                    "  }, \n" +
                                    "  \"name\": \"user-test1\",\n" +
                                    "  \"classifier\": {\n" +
                                    "    \"name\": \"orders\",\n" +
                                    "    \"namespace\": \"cloudbss-kube-core-demo-2\",\n" +
                                    "    \"tenantId\": \"d047619f-6886-4842-81a7-3f87cb748ac1\"\n" +
                                    "  }, \n" +
                                    "  \"namespace\": \"cloudbss-kube-core-demo-2\",\n" +
                                    "  \"instance\": \"default\",\n" +
                                    "  \"requestedSettings\": {\n" +
                                    "    \"numPartitions\": 0,\n" +
                                    "    \"minNumPartitions\": 2,\n" +
                                    "    \"replicationFactor\": 1,\n" +
                                    "    \"replicaAssignment\": null,\n" +
                                    "    \"configs\": null\n" +
                                    "  },\n" +
                                    "  \"actualSettings\": {\n" +
                                    "    \"numPartitions\": 1,\n" +
                                    "    \"replicationFactor\": 1,\n" +
                                    "    \"replicaAssignment\": {\n" +
                                    "      \"0\": [ 0 ]\n" +
                                    "    },\n" +
                                    "    \"configs\": {}\n" +
                                    "  }\n" +
                                    "}\n")
            );

            // run test
            var client = createKafkaClient("http://localhost:" + mockServer.getPort());

            TopicAddress topicAddress = client.getOrCreateTopic(new Classifier("orders"),
                    TopicCreateOptions.builder()
                            .onTopicExists(OnTopicExists.MERGE)
                            .name("user-test1")
                            .minNumPartitions(2)
                            .build());
            assertNotNull(topicAddress);
            assertEquals("user-test1", topicAddress.getTopicName());
            assertEquals("my-kafka.kafka-cluster:9092", topicAddress.getBoostrapServers("PLAINTEXT"));
        });
    }

    @Test
    void testWatchTopicCreate(ClientAndServer mockServer) throws InterruptedException {
        withProp(Env.PROP_NAMESPACE, "cloud-dev", () -> {
            withProp(Env.PROP_API_URL, "http://localhost:" + mockServer.getPort(), () -> {

                HttpRequest req = request().withMethod("POST").withPath("/api/v2/kafka/topic/watch-create");
                ExpectationResponseCallback respWithError = httpRequest -> {
                    throw new RuntimeException("Some connection problem");
                };

                ExpectationResponseCallback longNothing = httpRequest -> {
                    Thread.sleep(1000);
                    return response().withBody("[]");
                };

                mockServer.when(request().withPath("/api-version")).respond(response().withBody("{\"major\":2, \"minor\":8}"));
                mockServer.when(req, Times.once()).respond(respWithError);
                mockServer.when(req, Times.once()).respond(longNothing);
                mockServer.when(req, Times.once()).respond(response().withBody(readResourceAsString("watch-response1.json")));
                mockServer.when(req, Times.once()).respond(longNothing);
                mockServer.when(req, Times.once()).respond(response().withBody(readResourceAsString("watch-response2.json")));

                KafkaMaaSClient kafkaClient = createKafkaClient("http://localhost:" + mockServer.getPort());

                final CountDownLatch awaiter1 = new CountDownLatch(1);
                kafkaClient.watchTopicCreate("orders", addr -> awaiter1.countDown());
                assertTrue(awaiter1.await(5, TimeUnit.SECONDS));

                final CountDownLatch awaiter2 = new CountDownLatch(1);
                kafkaClient.watchTopicCreate("issues", addr -> awaiter2.countDown());
                assertTrue(awaiter2.await(5, TimeUnit.SECONDS));
            });
        });
    }

    @Test
    void testTopicDeleteSuccess(ClientAndServer mockServer) throws Exception {
        withProp(Env.PROP_NAMESPACE, "cloud-dev", () -> {
            withProp(Env.PROP_API_URL, "http://localhost:" + mockServer.getPort(), () -> {

                mockServer.when(
                        request().withMethod("DELETE").withPath("/api/v2/kafka/topic"), Times.once()
                ).respond(
                        response().withBody(Utils.readResourceAsString("topic-delete-resp.json"))
                );
                mockServer.when(
                        request().withMethod("DELETE").withPath("/api/v2/kafka/topic"), Times.once()
                ).respond(
                        response().withBody("{\"deletedSuccessfully\": [], \"failedToDelete\": []}")
                );

                KafkaMaaSClient kafkaClient = new MaaSAPIClientImpl(() -> "faketoken", null, null).getKafkaClient();
                assertTrue(kafkaClient.deleteTopic(new Classifier("orders")));
                assertFalse(kafkaClient.deleteTopic(new Classifier("orders")));
            });
        });
    }

    @Test
    void testTopicDeleteError(ClientAndServer mockServer) throws Exception {
        withProp(Env.PROP_NAMESPACE, "cloud-dev", () -> {
            withProp(Env.PROP_API_URL, "http://localhost:" + mockServer.getPort(), () -> {

                mockServer.when(
                        request().withMethod("DELETE").withPath("/api/v2/kafka/topic"),
                        Times.once()
                ).respond(response().withBody("{\n" +
                                "  \"deletedSuccessfully\": [],\n" +
                                "  \"failedToDelete\": [{\"message\": \"oops\"}]\n" +
                                "}"
                        )
                );

                mockServer.when(
                        request().withMethod("DELETE").withPath("/api/v2/kafka/topic"),
                        Times.once()
                ).respond(response().withBody("{\"deletedSuccessfully\": [], \"failedToDelete\": []}"));

                KafkaMaaSClient kafkaClient = createKafkaClient("http://localhost:" + mockServer.getPort());
                assertThrows(MaaSException.class, () -> kafkaClient.deleteTopic(new Classifier("orders")));
            });
        });
    }


    @Test
    public void testDeleteTemplate(ClientAndServer mockServer) {
        // prepare environment
        withProp(Env.PROP_NAMESPACE, "cloudbss-kube-core-demo-2", () -> {
            mockServer.when(
                    request()
                            .withPath("/api/v2/kafka/topic-template")
                            .withMethod("DELETE")
                            .withHeader("authorization", "Bearer faketoken")
                            .withBody(
                                    json("{\n" +
                                            "    \"name\": \"my-template\"\n" +
                                            "}"
                                    )),
                    Times.once()
            ).respond(
                    response()
                            .withStatusCode(200)
                            .withBody("{\n" +
                                    "    \"name\": \"my-template\",\n" +
                                    "    \"namespace\": \"namespace\",\n" +
                                    "    \"numPartitions\": 1,\n" +
                                    "    \"replicationFactor\": 1,\n" +
                                    "    \"configs\": {\n" +
                                    "        \"flush.ms\": \"1088\"\n" +
                                    "    }\n" +
                                    "}")
            );

            // run test
            var client = createKafkaClient("http://localhost:" + mockServer.getPort());

            TopicTemplate deletedTemplate = client.deleteTopicTemplate("my-template");
            assertNotNull(deletedTemplate);
            assertEquals("my-template", deletedTemplate.getName());
        });
    }

    @Test
    void testApiVersionSwitch(ClientAndServer mockServer) throws IOException {
        withProp(Env.PROP_NAMESPACE, "cloudbss-kube-core-demo-2", () -> {
            withProp(Env.PROP_HTTP_TIMEOUT, "1", () -> {
                mockServer.reset();
                mockServer.when(request().withPath("/api/v1/kafka/topic"), Times.once())
                        .respond(response()
                                .withStatusCode(200)
                                .withBody("{\n" +
                                        "  \"name\": \"topic-from-v1\"\n" +
                                        "}\n")
                        );

                mockServer.when(request().withPath("/api/v2/kafka/topic"), Times.once())
                        .respond(response()
                                .withStatusCode(200)
                                .withBody("{\n" +
                                        "  \"name\": \"topic-from-v2\"\n" +
                                        "}\n")
                        );

                mockServer.when(request().withPath("/api-version"), Times.once())
                        .respond(response().withBody("{\"major\":2, \"minor\": 15}"));


                var client = createKafkaClient("http://localhost:" + mockServer.getPort());
                TopicAddress topicAddress = client.getOrCreateTopic(new Classifier("orders"),
                        TopicCreateOptions.builder()
                                .name("test")
                                .build());
                assertEquals("topic-from-v1", topicAddress.getTopicName());

                mockServer.when(request().withPath("/api-version"), Times.once())
                        .respond(response().withBody("{\"major\":2, \"minor\": 16}"));

                client = createKafkaClient("http://localhost:" + mockServer.getPort());
                topicAddress = client.getOrCreateTopic(new Classifier("orders"),
                        TopicCreateOptions.builder()
                                .name("test")
                                .build());
                assertEquals("topic-from-v2", topicAddress.getTopicName());
            });
        });
    }

    @Test
    void testSearchTopic(ClientAndServer mockServer) {
        mockServer.when(
                request()
                        .withPath("/api/v2/kafka/topic/search")
                        .withMethod("POST")
                        .withHeader("authorization", "Bearer faketoken")
                        .withBody(json("""
                                {"topic": "abc"}
                        """)),
                Times.once()
        ).respond(
                response()
                        .withStatusCode(200)
                        .withBody("""
                                [
                                    {
                                      "addresses": {
                                        "PLAINTEXT": [
                                          "localhost:9092"
                                        ]
                                      },
                                      "name": "maas.core-dev.test1",
                                      "classifier": {
                                        "name": "test1",
                                        "namespace": "core-dev"
                                      },
                                      "namespace": "core-dev",
                                      "externallyManaged": false,
                                      "instance": "local",
                                      "requestedSettings": {
                                        "numPartitions": 0,
                                        "replicationFactor": 0
                                      },
                                      "actualSettings": {
                                        "numPartitions": 1,
                                        "replicationFactor": 1,
                                        "replicaAssignment": { "0": [ 0 ] },
                                        "configs": {
                                          "cleanup.policy": "delete"
                                        }
                                      }
                                    }
                                  ]
                                """)
        );

        var client = createKafkaClient("http://localhost:" + mockServer.getPort());
        var result = client.search(SearchCriteria.builder().topic("abc").build());
        assertEquals(1, result.size());
    }

    private KafkaMaaSClientImpl createKafkaClient(String agentUrl) {
        var httpClient = new HttpClient(() -> "faketoken");
        var serverApiVersion = new ServerApiVersion(httpClient, agentUrl);

        return new KafkaMaaSClientImpl(
                httpClient,
                null,
                new ApiUrlProvider(serverApiVersion, agentUrl));
    }
}
