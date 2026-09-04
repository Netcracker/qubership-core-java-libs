package com.netcracker.cloud.maas.client.impl.rabbit;

import com.netcracker.cloud.maas.client.api.Classifier;
import com.netcracker.cloud.maas.client.api.MaaSHttpException;
import com.netcracker.cloud.maas.client.impl.ApiUrlProvider;
import com.netcracker.cloud.maas.client.impl.Env;
import com.netcracker.cloud.maas.client.impl.apiversion.ServerApiVersion;
import com.netcracker.cloud.maas.client.impl.http.HttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.matchers.Times;
import org.mockserver.verify.VerificationTimes;

import static com.netcracker.cloud.maas.client.Utils.withProp;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@ExtendWith(MockServerExtension.class)
class RabbitFailoverTest {

    private static final String PATH = "/api/v2/rabbit/vhost";

    private String savedAgentUrl;

    @BeforeEach
    void reset(ClientAndServer mockServer) {
        savedAgentUrl = System.getProperty(Env.PROP_MAAS_AGENT_URL);
        mockServer.reset();
        mockServer.when(request().withPath("/api-version"))
                .respond(response().withBody("{\"major\":2, \"minor\": 16}"));
    }

    // the agent url points at a mock server port that is gone once this class is done,
    // so it must not leak into the rest of the JVM
    @AfterEach
    void restoreAgentUrl() {
        if (savedAgentUrl == null) {
            System.clearProperty(Env.PROP_MAAS_AGENT_URL);
        } else {
            System.setProperty(Env.PROP_MAAS_AGENT_URL, savedAgentUrl);
        }
    }

    @ParameterizedTest(name = "{0} is retried on the vhost path")
    @CsvSource(delimiter = '|', textBlock = """
            a read-only database | 405 | {"code":"MAAS-0600","reason":"database is in read-only mode"}
            an unreachable agent | 500 | {"error":"error proxying request: connection refused"}
            """)
    void testFailover_RetryableResponseSucceedsOnRetry(String description, int status, String body,
                                                       ClientAndServer mockServer) {
        mockServer.when(request().withMethod("POST").withPath(PATH), Times.exactly(2))
                .respond(response().withStatusCode(status).withBody(body));
        mockServer.when(request().withMethod("POST").withPath(PATH), Times.unlimited())
                .respond(response().withStatusCode(200).withBody("""
                        {
                          "cnn": "ampq://rabbit-cluster:4321/maas.core-dev.123456",
                          "username": "testuser",
                          "password": "plain:testpassword"
                        }
                        """));

        withProp(Env.PROP_NAMESPACE, "core-dev", () ->
                withFastRetries(() -> {
                    RabbitMaaSClientImpl client = createRabbitClient("http://localhost:" + mockServer.getPort());
                    assertNotNull(client.getOrCreateVirtualHost(new Classifier("commands")));
                }));

        mockServer.verify(request().withMethod("POST").withPath(PATH), VerificationTimes.exactly(3));
    }

    @Test
    void testFailover_400NotRetried(ClientAndServer mockServer) {
        mockServer.when(request().withMethod("POST").withPath(PATH), Times.unlimited())
                .respond(response().withStatusCode(400).withBody("{\"error\":\"bad request\"}"));

        withProp(Env.PROP_NAMESPACE, "core-dev", () ->
                withFastRetries(() -> {
                    RabbitMaaSClientImpl client = createRabbitClient("http://localhost:" + mockServer.getPort());
                    Classifier classifier = new Classifier("commands");
                    assertThrows(MaaSHttpException.class, () -> client.getOrCreateVirtualHost(classifier));
                }));

        mockServer.verify(request().withMethod("POST").withPath(PATH), VerificationTimes.exactly(1));
    }

    // A short total duration is now the only lever: it bounds both the number of attempts
    // and the pauses between them (the cap is derived as a quarter of it).
    private static void withFastRetries(Runnable test) {
        withProp(Env.PROP_HTTP_RETRY_MAX_TOTAL_DURATION_MS, "5000", test::run);
    }

    private static RabbitMaaSClientImpl createRabbitClient(String agentUrl) {
        System.setProperty(Env.PROP_MAAS_AGENT_URL, agentUrl);
        var httpClient = HttpClient.getMaasClient(() -> "faketoken");
        var serverApiVersion = new ServerApiVersion(httpClient, agentUrl);
        return new RabbitMaaSClientImpl(httpClient, new ApiUrlProvider(serverApiVersion, agentUrl));
    }
}
