package org.qubership.cloud.restlegacy.resttemplate.app;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.framework.contexts.tenant.TenantContextObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestTemplate;

import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.qubership.cloud.framework.contexts.tenant.BaseTenantProvider.TENANT_CONTEXT_NAME;


@SpringBootTest(classes = TestConfig.class,
        properties = {"apigateway.url=http://localhost:18283"}
)
class RestTemplateTest {

    private static HttpServer httpServer;

    @Autowired
    @Qualifier("restTemplate")
    private RestTemplate restTemplate;

    private static final String FULL_URL = "http://localhost:18283/api/v1/test-app/test";

    @BeforeAll
    static void initServer() throws Exception {
        ContextManager.set(TENANT_CONTEXT_NAME, new TenantContextObject(""));
        httpServer = HttpServer.create(new InetSocketAddress(18283), 0);

        httpServer.createContext("/api/v1/test-app/test", exchange -> {
            byte[] response;
            int responseCode;
            System.out.println("request body: " + new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            switch (exchange.getRequestMethod()) {
                case "OPTIONS":
                    response = null;
                    responseCode = HttpURLConnection.HTTP_NO_CONTENT;
                    exchange.getResponseHeaders().add("Allow", "GET, POST, PUT, PATCH, DELETE, OPTIONS, HEAD");
                    break;
                case "HEAD":
                    response = null;
                    responseCode = HttpURLConnection.HTTP_NO_CONTENT;
                    exchange.getResponseHeaders().add("test-header-name", "test-header-value");
                    break;
                default:
                    response = "{\"success\": true}".getBytes();
                    responseCode = HttpURLConnection.HTTP_OK;
                    break;
            }
            exchange.sendResponseHeaders(responseCode, response == null ? -1 : response.length);
            if (response != null) {
                exchange.getResponseBody().write(response);
            }
            exchange.close();
        });
        httpServer.start();
    }

    @AfterAll
    static void stopHttpServer() {
        httpServer.stop(0);
    }

    @Test
    void testSyncRestTemplateGet() {
        assertNotNull(restTemplate.getForEntity(FULL_URL, String.class));
    }


    @Test
    void testSyncRestTemplatePost() {
        assertNotNull(restTemplate.postForEntity(FULL_URL, null, String.class));
    }

    @Test
    void testSyncRestTemplatePatch() {
        assertNotNull(restTemplate.patchForObject(FULL_URL, null, String.class));
    }

    @Test
    void testSyncRestTemplateDelete() {
        restTemplate.delete(FULL_URL);
    }

    @Test
    void testSyncRestTemplateOptional() {
        assertNotNull(restTemplate.optionsForAllow(FULL_URL));
    }

    @Test
    void testSyncRestTemplateHead() {
        assertNotNull(restTemplate.headForHeaders(FULL_URL));
    }
}
