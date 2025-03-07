package org.qubership.cloud.restlegacy.resttemplate.app;

import org.qubership.cloud.context.propagation.core.ContextManager;
import com.sun.net.httpserver.HttpServer;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.qubership.cloud.framework.contexts.tenant.TenantContextObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.qubership.cloud.framework.contexts.tenant.TenantProvider.TENANT_CONTEXT_NAME;


@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {TestConfig.class})
@TestPropertySource(properties = {
        "apigateway.url=http://localhost:18283"
})
public class RestTemplateTest {

    private static HttpServer httpServer;

    @Autowired
    @Qualifier("restTemplate")
    private RestTemplate restTemplate;

    private static final String FULL_URL = "http://localhost:18283/api/v1/test-app/test";

    @BeforeClass
    public static void initServer() throws Exception {
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

    @AfterClass
    public static void stopHttpServer() {
        httpServer.stop(0);
    }

    @Test
    public void testSyncRestTemplateGet() {
        Assert.assertNotNull(restTemplate.getForEntity(FULL_URL, String.class));
    }


    @Test
    public void testSyncRestTemplatePost() {
        Assert.assertNotNull(restTemplate.postForEntity(FULL_URL, null, String.class));
    }


    @Test
    public void testSyncRestTemplatePatch() {
        Assert.assertNotNull(restTemplate.patchForObject(FULL_URL, null, String.class));
    }

    @Test
    public void testSyncRestTemplateDelete() {
        restTemplate.delete(FULL_URL);
    }
    @Test
    public void testSyncRestTemplateOptional() {
        Assert.assertNotNull(restTemplate.optionsForAllow(FULL_URL));
    }

    @Test
    public void testSyncRestTemplateHead() {
        Assert.assertNotNull(restTemplate.headForHeaders(FULL_URL));
    }
}