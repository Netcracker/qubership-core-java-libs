package com.netcracker.cloud.restlegacy.restclient.app;

import com.sun.net.httpserver.HttpServer;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import com.netcracker.cloud.context.propagation.core.ContextManager;
import com.netcracker.cloud.framework.contexts.tenant.TenantContextObject;
import com.netcracker.cloud.restlegacy.restclient.ApiGatewayClient;
import com.netcracker.cloud.restlegacy.resttemplate.RestTemplateFactory;
import com.netcracker.cloud.restlegacy.resttemplate.configuration.RestTemplateConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static com.netcracker.cloud.framework.contexts.tenant.BaseTenantProvider.TENANT_CONTEXT_NAME;


@SpringBootTest(classes = {TestConfig.class, RestTemplateConfiguration.class},
        properties = {"apigateway.url=http://localhost:18292"})
class RestServerTest {

    private static HttpServer httpServer;
    @Autowired
    private ApiGatewayClient apiGatewayClient;
    @Autowired
    private RestTemplateFactory restTemplateFactory;
    private String url = "/test";

    protected final static RetryPolicy<Object> DEFAULT_RETRY_POLICY = new RetryPolicy<>()
            .withMaxRetries(-1).withDelay(Duration.ofMillis(500)).withMaxDuration(Duration.ofSeconds(10));

    @BeforeAll
    static void initServer() throws Exception {
        ContextManager.set(TENANT_CONTEXT_NAME, new TenantContextObject(""));
        httpServer = HttpServer.create(new InetSocketAddress(18292), 0);

        httpServer.createContext("/api/v1/test-app/test", exchange -> {
            byte[] response;
            int responseCode;
            switch (exchange.getRequestMethod()) {
                case "PATCH":
                    response = null;
                    responseCode = HttpURLConnection.HTTP_NO_CONTENT;
                    break;
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
    void testSyncGet() {
        assertNotNull(apiGatewayClient.get(url, String.class));
    }

    @Test
    void testSyncPost() {
        Failsafe.with(DEFAULT_RETRY_POLICY).run(() ->
                assertNotNull(apiGatewayClient.post(url, null, String.class)));
    }

    @Test
    void testSyncPatch() {
        apiGatewayClient.patch(url, null, String.class);
    }

    @Test
    void testSyncDelete() {
        apiGatewayClient.delete(url);
    }

    @Test
    void testSyncOptions() {
        Set<HttpMethod> httpMethods = apiGatewayClient.options(url);
        assertNotNull(httpMethods);
    }

    @Test
    void testSyncHead() {
        HttpHeaders httpHeaders = apiGatewayClient.head(url);
        assertNotNull(httpHeaders);
    }
}
