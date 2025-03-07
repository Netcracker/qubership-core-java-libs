package org.qubership.cloud.dbaas.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import org.qubership.cloud.dbaas.client.entity.ApiVersionInfo;
import org.qubership.cloud.dbaas.client.entity.connection.PostgresDBConnection;
import org.qubership.cloud.dbaas.client.entity.database.PostgresDatabase;
import org.qubership.cloud.dbaas.client.entity.database.type.PostgresDBType;
import org.qubership.cloud.dbaas.client.exceptions.DbaasException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.RequestDefinition;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.MediaType.APPLICATION_JSON;

@ExtendWith(MockitoExtension.class)
class DbaaSClientOkHttpImplEnhancedTest {

    public static final String TEST_DBAAS_AGENT_URL_TEMPLATE = "http://localhost:%d";
    public static final String TEST_NAMESPACE = "test-ns";
    private static ObjectMapper objectMapper = new ObjectMapper();
    private ClientAndServer mockServer;
    private OkHttpClient httpClient;
    private DbaaSClientOkHttpImpl dbaaSClientCommon;
    private String dbaasAgentUrl;

    @BeforeEach
    public void setUp() throws IOException {
        final Duration timeout = Duration.ofMinutes(1);
        httpClient = new OkHttpClient.Builder()
                .callTimeout(timeout)
                .readTimeout(timeout)
                .writeTimeout(timeout)
                .connectTimeout(timeout)
                .build();
        mockServer = startClientAndServer();
        dbaasAgentUrl = String.format(TEST_DBAAS_AGENT_URL_TEMPLATE, mockServer.getLocalPort());
        dbaaSClientCommon = new DbaaSClientOkHttpImpl(dbaasAgentUrl, httpClient);
    }

    @AfterEach
    void cleanup() {
        mockServer.stop();
    }

    @Test
    void testSuccessAfterCoupleFails() throws Exception {
        Map<String, Object> classifier = new HashMap<>();
        classifier.put("scope", "service");
        classifier.put("microserviceName", "test-service");
        classifier.put("namespace", TEST_NAMESPACE);

        PostgresDatabase expectedDb = new PostgresDatabase();
        expectedDb.setClassifier(new TreeMap<>(classifier));
        expectedDb.setConnectionProperties(new PostgresDBConnection());

        RequestDefinition request = HttpRequest.request()
                .withMethod("PUT")
                .withPath(String.format("/api/v3/dbaas/%s/databases", TEST_NAMESPACE))
                .withContentType(APPLICATION_JSON);
        AtomicInteger triesCount = new AtomicInteger(0);
        mockServer.when(request).respond(httpRequest -> {
            if ((triesCount.incrementAndGet() < 2)) {
                return HttpResponse.response().withStatusCode(503);
            }
            return HttpResponse.response().withStatusCode(200).withBody(objectMapper.writeValueAsString(expectedDb));
        });

        RequestDefinition apiVersionRequest = HttpRequest.request()
                .withMethod("GET")
                .withPath("/api-version");
        mockServer.when(apiVersionRequest).respond(httpRequest -> {
            ApiVersionInfo apiVersionInfo = new ApiVersionInfo(3,1, List.of(3),createSpecsList());
            return HttpResponse.response().withStatusCode(200).withBody(objectMapper.writeValueAsString(apiVersionInfo));
        });

        final PostgresDatabase database = dbaaSClientCommon.getOrCreateDatabase(PostgresDBType.INSTANCE, TEST_NAMESPACE, classifier);
        Assertions.assertNotNull(database);
    }

    @Test
    void testNotSuccessReachMaximumNumberOfRetriesAmount() throws Exception {
        Map<String, Object> classifier = new HashMap<>();
        classifier.put("scope", "service");
        classifier.put("microserviceName", "test-service");
        classifier.put("namespace", TEST_NAMESPACE);

        PostgresDatabase expectedDb = new PostgresDatabase();
        expectedDb.setClassifier(new TreeMap<>(classifier));
        expectedDb.setConnectionProperties(new PostgresDBConnection());

        RequestDefinition request = HttpRequest.request()
                .withMethod("PUT")
                .withPath(String.format("/api/v3/dbaas/%s/databases", TEST_NAMESPACE))
                .withContentType(APPLICATION_JSON);

        mockServer.when(request).respond(httpRequest -> HttpResponse.response().withStatusCode(503));

        RequestDefinition apiVersionRequest = HttpRequest.request()
                .withMethod("GET")
                .withPath("/api-version");
        mockServer.when(apiVersionRequest).respond(httpRequest -> {
            ApiVersionInfo apiVersionInfo = new ApiVersionInfo(3,1,List.of(3),createSpecsList());
            return HttpResponse.response().withStatusCode(200).withBody(objectMapper.writeValueAsString(apiVersionInfo));
        });

        DbaasException exception = Assertions.assertThrows(DbaasException.class,
                () -> dbaaSClientCommon.getOrCreateDatabase(PostgresDBType.INSTANCE, TEST_NAMESPACE, classifier));
        Assertions.assertEquals("Has reached maximum number of retries amount", exception.getMessage());
    }

    @Test
    void testNotSuccessAfterCoupleFailsAndWithMismatchedInputException() throws Exception {
        Map<String, Object> classifier = new HashMap<>();
        classifier.put("scope", "service");
        classifier.put("microserviceName", "test-service");
        classifier.put("namespace", TEST_NAMESPACE);

        RequestDefinition request = HttpRequest.request()
                .withMethod("PUT")
                .withPath(String.format("/api/v3/dbaas/%s/databases", TEST_NAMESPACE))
                .withContentType(APPLICATION_JSON);
        AtomicInteger triesCount = new AtomicInteger(0);
        mockServer.when(request).respond(httpRequest -> {
            if ((triesCount.incrementAndGet() < 2)) {
                return HttpResponse.response().withStatusCode(503);
            }
            return HttpResponse.response().withStatusCode(200);
        });

        RequestDefinition apiVersionRequest = HttpRequest.request()
                .withMethod("GET")
                .withPath("/api-version");
        mockServer.when(apiVersionRequest).respond(httpRequest -> {
            ApiVersionInfo apiVersionInfo = new ApiVersionInfo(3,1,List.of(3),createSpecsList());
            return HttpResponse.response().withStatusCode(200).withBody(objectMapper.writeValueAsString(apiVersionInfo));
        });

        DbaasException exception = Assertions.assertThrows(DbaasException.class,
                () -> dbaaSClientCommon.getOrCreateDatabase(PostgresDBType.INSTANCE, TEST_NAMESPACE, classifier));
        Assertions.assertTrue(exception.getCause() instanceof MismatchedInputException);
    }


    private List<ApiVersionInfo.Info> createSpecsList() {
        ApiVersionInfo.Info apiSpec = new ApiVersionInfo.Info("/api", 3, 14, List.of(3));
        ApiVersionInfo.Info declarationsSpec = new ApiVersionInfo.Info("/api/declarations", 1, 0, List.of(1));
        ApiVersionInfo.Info compositeSpec = new ApiVersionInfo.Info("/api/composite", 1, 0, List.of(1));
        return List.of(apiSpec, declarationsSpec, compositeSpec);
    }

}
