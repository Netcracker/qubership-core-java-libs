package com.netcracker.cloud.smartclient.rest.webclient.sample;

import com.netcracker.cloud.context.propagation.core.ContextManager;
import com.netcracker.cloud.context.propagation.spring.webclient.interceptor.SpringWebClientInterceptor;
import com.netcracker.cloud.framework.contexts.tenant.TenantContextObject;
import com.netcracker.cloud.security.common.webclient.SmartWebClient;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Objects;

import static com.netcracker.cloud.framework.contexts.tenant.BaseTenantProvider.TENANT_CONTEXT_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {TestConfig.class})
class ApplicationTest {

    @Autowired
    SmartWebClient smartWebClient;

    @Autowired
    SpringWebClientInterceptor springWebClientInterceptor;

    private static HttpServer httpServer;

    @BeforeAll
    static void initServer() throws Exception {
        ContextManager.set(TENANT_CONTEXT_NAME, new TenantContextObject(""));
        httpServer = HttpServer.create(new InetSocketAddress(8181), 0);

        httpServer.createContext("/test", exchange -> {
            byte[] response;
            int responseCode;
            switch (exchange.getRequestMethod()) {
                case "OPTIONS":
                    response = null;
                    responseCode = HttpURLConnection.HTTP_OK;
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
            exchange.sendResponseHeaders(responseCode, response == null ? 0 : response.length);
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

    @Autowired
    private WebClient userWebClient;

    @Test
    void getWebClient() {
        assertEquals(HttpStatus.OK, Objects.requireNonNull(userWebClient.get().uri("http://localhost:8181/test").retrieve().toBodilessEntity().block()).getStatusCode());
    }

    @Test
    void postWebClient() {
        assertEquals(HttpStatus.OK, Objects.requireNonNull(userWebClient.post().uri("http://localhost:8181/test").retrieve().toBodilessEntity().block()).getStatusCode());
    }

    @Test
    void patchWebClient() {
        ResponseEntity<Void> response = userWebClient.patch().uri("http://localhost:8181/test").retrieve().toBodilessEntity().block();
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void deleteWebClient() {
        assertEquals(HttpStatus.OK, Objects.requireNonNull(userWebClient.delete().uri("http://localhost:8181/test").retrieve().toBodilessEntity().block()).getStatusCode());
    }

    @Test
    void optionWebClient() {
        assertEquals(HttpStatus.OK, Objects.requireNonNull(userWebClient.options().uri("http://localhost:8181/test").retrieve().toBodilessEntity().block()).getStatusCode());
    }

    @Test
    void headWebClient() {
        assertEquals(HttpStatus.NO_CONTENT, Objects.requireNonNull(userWebClient.head().uri("http://localhost:8181/test").retrieve().toBodilessEntity().block()).getStatusCode());
    }

    @Test
    void smartWebClientInterceptorsTest() throws NoSuchFieldException, IllegalAccessException {
        Field genericInterceptors = smartWebClient.getClass().getDeclaredField("genericInterceptors");
        genericInterceptors.setAccessible(true);
        List<ExchangeFilterFunction> interceptors = (List<ExchangeFilterFunction>) genericInterceptors.get(smartWebClient);
        assertTrue(interceptors.contains(springWebClientInterceptor));
    }
}
