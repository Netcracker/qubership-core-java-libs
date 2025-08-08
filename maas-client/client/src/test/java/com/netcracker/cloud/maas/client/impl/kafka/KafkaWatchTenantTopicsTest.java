package com.netcracker.cloud.maas.client.impl.kafka;

import org.qubership.cloud.maas.client.api.kafka.TopicAddress;
import org.qubership.cloud.maas.client.impl.ApiUrlProvider;
import org.qubership.cloud.maas.client.impl.Env;
import org.qubership.cloud.maas.client.impl.apiversion.ServerApiVersion;
import org.qubership.cloud.maas.client.impl.http.HttpClient;
import org.qubership.cloud.tenantmanager.client.impl.TenantManagerConnectorImpl;
import org.qubership.cloud.testharness.MaaSCocoonExtension;
import org.qubership.cloud.testharness.TenantManagerMockInject;
import org.qubership.cloud.testharness.TenantManagerMockServer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.matchers.MatchType;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.qubership.cloud.maas.client.Utils.withProp;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.JsonBody.json;

@ExtendWith({MockServerExtension.class, MaaSCocoonExtension.class})
@Slf4j
public class KafkaWatchTenantTopicsTest {
    @TenantManagerMockInject
	TenantManagerMockServer tmMock;

    @BeforeEach
	public void resetMOckServer(ClientAndServer mockServer) {
		mockServer.reset();
		mockServer.when(
				request()
						.withPath("/api-version")
		).respond(
				response().withBody("{\"major\":2, \"minor\": 16}")
		);
	}

    @Test
	public void testWatchEvents(ClientAndServer mockServer) throws Exception {
    	withProp(Env.PROP_NAMESPACE, "core-dev", () -> {
			HttpClient httpClient = new HttpClient(() -> "faketoken");
			var agentUrl = "http://localhost:" + mockServer.getPort();
			var serverApiVersion = new ServerApiVersion(httpClient, agentUrl);

			KafkaMaaSClientImpl client = new KafkaMaaSClientImpl(
					httpClient,
					() -> new TenantManagerConnectorImpl(tmMock.getUrl(), httpClient),
					new ApiUrlProvider(serverApiVersion, agentUrl));

			BlockingQueue<List<TopicAddress>> events = new LinkedBlockingDeque<>();
			Consumer<List<TopicAddress>> cb = topics -> {
				log.info(">>>> Topics received: {}", topics);
				events.add(topics);
			};
			client.watchTenantTopics("orders", cb);

			List<TopicAddress> topics = events.poll(1, TimeUnit.SECONDS);
			assertNotNull(topics);
			assertEquals(0, topics.size());


			// expect http call to maas
			mockServer.when(
					request()
							.withMethod("POST")
							.withPath("/api/v2/kafka/topic/get-by-classifier")
							.withHeader("authorization", "Bearer faketoken")
							.withBody(
									json("{\"name\": \"orders\"," +
													"\"namespace\": \"core-dev\"," +
													"\"tenantId\": \"233a1c4c-dde1-4766-9ca5-c0b21400c2b7\"}"
											, MatchType.STRICT)
							)
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
									"    \"tenantId\": \"233a1c4c-dde1-4766-9ca5-c0b21400c2b7\"\n" +
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

			tmMock.addFirstActivatedTenant();
			topics = events.poll(1, TimeUnit.SECONDS);
			assertNotNull(topics);
			assertEquals(1, topics.size());
			assertEquals("maas.core_dev.orders.1234567", topics.get(0).getTopicName());
		});
	}

	@Test
	public void testWatchEvents_ButTopicsNotFoundInMaaS(ClientAndServer mockServer) throws Exception {
		withProp(Env.PROP_NAMESPACE,  "core-dev", () -> {
			HttpClient httpClient = new HttpClient(() -> "faketoken");
			var agentUrl = "http://localhost:" + mockServer.getPort();
			KafkaMaaSClientImpl client = new KafkaMaaSClientImpl(
					httpClient,
					() -> new TenantManagerConnectorImpl(tmMock.getUrl(), httpClient),
					new ApiUrlProvider(new ServerApiVersion(httpClient, agentUrl), agentUrl));

			BlockingQueue<List<TopicAddress>> events = new LinkedBlockingDeque<>();
			Consumer<List<TopicAddress>> cb = topics -> {
				log.info(">>>> Topics received: {}", topics);
				events.add(topics);
			};
			client.watchTenantTopics("orders", cb);

			List<TopicAddress> topics = events.poll(1, TimeUnit.SECONDS);
			assertNotNull(topics);
			assertEquals(0, topics.size());


			// expect http call to maas
			mockServer.when(
					request()
							.withMethod("POST")
							.withPath("/api/v2/kafka/topic/get-by-classifier")
							.withHeader("authorization", "Bearer faketoken")
							.withBody(
									json("{\"name\": \"orders\"," +
													"\"namespace\": \"core-dev\"," +
													"\"tenantId\": \"233a1c4c-dde1-4766-9ca5-c0b21400c2b7\"}"
											, MatchType.STRICT)
							)
			).respond(
					response().withStatusCode(404)
			);

			tmMock.addFirstActivatedTenant();
			topics = events.poll(1, TimeUnit.SECONDS);
			assertNotNull(topics);
			assertEquals(0, topics.size());
		});
	}
}
