package com.netcracker.cloud.maas.client.impl.rabbit;

import org.qubership.cloud.bluegreen.impl.service.LocalDevBlueGreenStatePublisher;
import org.qubership.cloud.maas.client.api.Classifier;
import org.qubership.cloud.maas.client.api.rabbit.VHost;
import org.qubership.cloud.maas.client.impl.ApiUrlProvider;
import org.qubership.cloud.maas.client.impl.Env;
import org.qubership.cloud.maas.client.impl.apiversion.ServerApiVersion;
import org.qubership.cloud.maas.client.impl.http.HttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.matchers.MatchType;

import java.util.UUID;

import static org.qubership.cloud.maas.client.Utils.withProp;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.JsonBody.json;

@ExtendWith({MockServerExtension.class})
class RabbitMaaSClientImplTest {

	@BeforeEach
	void reset(ClientAndServer mockServer){
		mockServer.reset();
        mockServer.when(
                request()
                        .withPath("/api-version")
        ).respond(
                response().withBody("{\"major\":2, \"minor\": 16}")
        );
	}

    @Test
    public void testGerOrCreateVHost(ClientAndServer mockServer) {
        withProp(Env.PROP_NAMESPACE, "core-dev", () -> {

            mockServer.when(
                    request()
                            .withMethod("POST")
                            .withPath("/api/v2/rabbit/vhost")
                            .withHeader("authorization", "Bearer faketoken")
                            .withBody(
                                    json("{\"classifier\": {\"name\": \"commands\", \"namespace\": \"core-dev\"}}",
                                            MatchType.STRICT)
                            )
            ).respond(
                    response()
                            .withStatusCode(200)
                            .withBody("""
                                    {
                                      "cnn": "ampq://rabbit-cluster:4321/maas.core-dev.123456",
                                      "apiUrl": "http://rabbit-cluster:15672/api",
                                      "username": "scott",
                                      "password": "plain:tiger"
                                    }
                                    """)
            );

            RabbitMaaSClientImpl client = createRabbitClient("http://localhost:" + mockServer.getPort());

            VHost vhost = client.getOrCreateVirtualHost(new Classifier("commands"));
            assertNotNull(vhost);
            assertEquals("ampq://rabbit-cluster:4321/maas.core-dev.123456", vhost.getCnn());
            assertEquals("http://rabbit-cluster:15672/api", vhost.getApiUrl());
            assertEquals("scott", vhost.getUsername());
            assertEquals("tiger", vhost.getPassword());
        });
    }

    @Test
    void testGerOrCreateVHost_UnknownFields(ClientAndServer mockServer) {
        withProp(Env.PROP_NAMESPACE, "core-dev", () -> {
            mockServer.when(
                    request()
                            .withMethod("POST")
                            .withPath("/api/v2/rabbit/vhost")
                            .withHeader("authorization", "Bearer faketoken")
                            .withBody(
                                    json("{\"classifier\": {\"name\": \"commands\", \"namespace\": \"core-dev\"}}",
                                            MatchType.STRICT)
                            )
            ).respond(
                    response()
                            .withStatusCode(200)
                            .withBody("""
                                    {
                                      "cnn": "ampq://rabbit-cluster:4321/maas.core-dev.123456",
                                      "unknown": "unknown",
                                      "username": "scott",
                                      "password": "plain:tiger"
                                    }
                                    """)
            );

            RabbitMaaSClientImpl client = createRabbitClient("http://localhost:" + mockServer.getPort());
            assertDoesNotThrow(() -> client.getOrCreateVirtualHost(new Classifier("commands")));
        });
    }

    @Test
    public void testGetVHost(ClientAndServer mockServer) {
        System.setProperty(Env.PROP_NAMESPACE, "core-dev");

        mockServer.when(
                request()
                        .withMethod("POST")
                        .withPath("/api/v2/rabbit/vhost/get-by-classifier")
                        .withQueryStringParameter("extended", "true")
                        .withHeader("authorization", "Bearer faketoken")
                        .withBody(
                                json("{\"name\": \"commands\", \"namespace\": \"core-dev\"}",
                                        MatchType.STRICT)
                        )
        ).respond(
                response()
                        .withStatusCode(200)
                        .withBody("{\n" +
                                "    \"vhost\": {\n" +
                                "        \"cnn\": \"ampq://rabbit-cluster:4321/maas.core-dev.123456\",\n" +
                                "        \"username\": \"scott\",\n" +
                                "        \"password\": \"plain:tiger\",\n" +
                                "        \"apiUrl\": \"http://rabbit-cluster:15672/api\"\n" +
                                "    },\n" +
                                "    \"entities\": {\n" +
                                "        \"exchanges\": [\n" +
                                "            {\n" +
                                "                \"name\": \"exchange\",\n" +
                                "                \"type\": \"direct\"\n" +
                                "            }\n" +
                                "        ]\n" +
                                "    }\n" +
                                "}\n")
        );

        RabbitMaaSClientImpl client = createRabbitClient("http://localhost:" + mockServer.getPort());

        VHost vhost = client.getVirtualHost(new Classifier("commands"));
        assertNotNull(vhost);
        assertEquals("ampq://rabbit-cluster:4321/maas.core-dev.123456", vhost.getCnn());
        assertEquals("scott", vhost.getUsername());
        assertEquals("tiger", vhost.getPassword());
        assertEquals("http://rabbit-cluster:15672/api", vhost.getApiUrl());
    }

    @Test
    public void testGetVHost_NotFound(ClientAndServer mockServer) {
        System.setProperty(Env.PROP_NAMESPACE, "core-dev");

        mockServer.when(
                request()
                        .withMethod("POST")
                        .withPath("/api/v2/rabbit/vhost/get-by-classifier")
                        .withHeader("authorization", "Bearer faketoken")
                        .withBody(
                                json("{\"name\": \"commands\", \"namespace\": \"core-dev\"}",
                                        MatchType.STRICT)
                        )
        ).respond(
                response()
                        .withStatusCode(404)
        );

        RabbitMaaSClientImpl client = createRabbitClient("http://localhost:" + mockServer.getPort());

        VHost vhost = client.getVirtualHost(new Classifier("commands"));
        assertNull(vhost);
    }

    @Test
    public void testTenantVHost(ClientAndServer mockServer) {
        UUID tenantId = UUID.randomUUID();
        System.setProperty(Env.PROP_NAMESPACE, "core-dev");

        mockServer.when(
                request()
                        .withMethod("POST")
                        .withPath("/api/v2/rabbit/vhost")
                        .withHeader("authorization", "Bearer faketoken")
                        .withBody(
                                json("{\"classifier\": {" +
                                                "\"name\": \"commands\", " +
                                                "\"namespace\": \"core-dev\", " +
                                                "\"tenantId\": \"" + tenantId + "\"" +
                                                "}}",
                                        MatchType.STRICT)
                        )
        ).respond(
                response()
                        .withStatusCode(201)
                        .withBody("{\n" +
                                "  \"cnn\": \"ampq://rabbit-cluster:4321/maas.core-dev.123456\",\n" +
                                "  \"username\": \"scott\",\n" +
                                "  \"password\": \"plain:tiger\"\n" +
                                "}\n")
        );

        RabbitMaaSClientImpl client = createRabbitClient("http://localhost:" + mockServer.getPort());

        VHost vhost = client.getVHost("commands", tenantId.toString());
        assertNotNull(vhost);
        assertEquals("ampq://rabbit-cluster:4321/maas.core-dev.123456", vhost.getCnn());
        assertEquals("scott", vhost.getUsername());
        assertEquals("tiger", vhost.getPassword());
    }

    @Test
    public void testVersionedQueueName1() {
        RabbitMaaSClientImpl client = new RabbitMaaSClientImpl(null, null);

        assertEquals("abc-v1", client.getVersionedQueueName("abc", new LocalDevBlueGreenStatePublisher("test-namespace")));
    }

    private RabbitMaaSClientImpl createRabbitClient(String agentUrl) {
        var httpClient = new HttpClient(() -> "faketoken");
        var serverApiVersion = new ServerApiVersion(httpClient, agentUrl);

        return new RabbitMaaSClientImpl(httpClient, new ApiUrlProvider(serverApiVersion, agentUrl));
    }
}
