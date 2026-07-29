package com.netcracker.cloud.security.core.utils.k8s.localdev;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenRequestClientTest {

    private HttpServer server;
    private String baseUrl;
    private final AtomicReference<String> lastBody = new AtomicReference<>();
    private final AtomicReference<String> lastAuth = new AtomicReference<>();
    private int responseStatus = 201;
    private String responseBody = """
            {
              "status": {
                "token": "minted-token",
                "expirationTimestamp": "2099-01-01T00:00:00Z"
              }
            }
            """;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> {
            lastAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(responseStatus, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void requestTokenSuccess() {
        TokenRequestClient client = new TokenRequestClient(baseUrl, "kube-user-token", HttpClient.newHttpClient());
        TokenRequestClient.TokenRequestResult result = client.requestToken("ns1", "my-sa", "netcracker");

        assertEquals("minted-token", result.token());
        assertEquals(Instant.parse("2099-01-01T00:00:00Z"), result.expiresAt());
        assertEquals("Bearer kube-user-token", lastAuth.get());
        assertTrue(lastBody.get().contains("\"netcracker\""));
        assertTrue(lastBody.get().contains("28800"));
    }

    @Test
    void requestTokenUnauthorized() {
        responseStatus = 403;
        responseBody = "{\"message\":\"forbidden\"}";
        TokenRequestClient client = new TokenRequestClient(baseUrl, "kube-user-token", HttpClient.newHttpClient());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> client.requestToken("ns1", "my-sa", "netcracker"));
        assertTrue(ex.getMessage().contains("unauthorized") || ex.getMessage().contains("403"));
        assertTrue(ex.getMessage().contains("RBAC"));
    }
}
