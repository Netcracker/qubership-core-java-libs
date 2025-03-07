package org.qubership.cloud.restlegacy.restclient.app;

import org.qubership.cloud.framework.contexts.tenant.TenantContextObject;
import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.restlegacy.restclient.ApiGatewayClient;
import org.qubership.cloud.restlegacy.resttemplate.RestTemplateFactory;
import org.qubership.cloud.restlegacy.resttemplate.configuration.RestTemplateConfiguration;
import com.sun.net.httpserver.HttpServer;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Set;

import static org.qubership.cloud.framework.contexts.tenant.TenantProvider.TENANT_CONTEXT_NAME;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestConfig.class, RestTemplateConfiguration.class})
@TestPropertySource(properties = {
        "apigateway.url=http://localhost:18292"
})
public class RestServerTest {

    private static HttpServer httpServer;
    @Autowired
    private ApiGatewayClient apiGatewayClient;
    @Autowired
    private RestTemplateFactory restTemplateFactory;
    private String url = "/test";

    protected final static RetryPolicy<Object> DEFAULT_RETRY_POLICY = new RetryPolicy<>()
            .withMaxRetries(-1).withDelay(Duration.ofMillis(500)).withMaxDuration(Duration.ofSeconds(10));

    @BeforeClass
    public static void initServer() throws Exception {
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

    @AfterClass
    public static void stopHttpServer() {
        httpServer.stop(0);
    }

    @Test
    public void testSyncGet() {
        Assert.assertNotNull(apiGatewayClient.get(url, String.class));
    }

    @Test
    public void testSyncPost(){
        Failsafe.with(DEFAULT_RETRY_POLICY).run(() ->
                Assert.assertNotNull(apiGatewayClient.post(url, null, String.class)));
    }
    @Test
    public void testSyncPatch() {
        apiGatewayClient.patch(url, null, String.class);
    }

    @Test
    public void testSyncDelete() {
        apiGatewayClient.delete(url);
    }

    @Test
    public void testSyncOptions() {
        Set<HttpMethod> httpMethods = apiGatewayClient.options(url);
        Assert.assertNotNull(httpMethods);
    }

    @Test
    public void testSyncHead() {
        HttpHeaders httpHeaders = apiGatewayClient.head(url);
        Assert.assertNotNull(httpHeaders);
    }
}