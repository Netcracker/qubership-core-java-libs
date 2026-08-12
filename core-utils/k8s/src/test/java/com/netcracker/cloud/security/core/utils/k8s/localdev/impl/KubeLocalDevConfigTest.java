package com.netcracker.cloud.security.core.utils.k8s.localdev.impl;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KubeLocalDevConfigTest {

    private HttpServer server;
    private String baseUrl;
    private final AtomicReference<String> lastAuth = new AtomicReference<>();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/.well-known/openid-configuration", exchange -> {
            lastAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] body = """
                    {"issuer":"https://kubernetes.default.svc","jwks_uri":"https://kubernetes.default.svc/openid/v1/jwks"}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
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
    void resolveIssuerFromDiscoveryWithoutAuth() {
        KubeLocalDevConfig config = configForServer(baseUrl);

        assertEquals("https://kubernetes.default.svc", config.resolveIssuerClaimFromDiscovery());
        assertEquals(null, lastAuth.get());
        assertEquals(baseUrl + KubeLocalDevConfig.JWKS_PATH, config.jwksUrl());
        assertEquals("kube-user-token", config.userToken());
    }

    @Test
    void isPublicOidcEndpointDetectsDiscoveryAndJwks() {
        KubeLocalDevConfig config = configForServer("https://api.example:6443");

        assertTrue(config.isPublicOidcEndpoint("https://api.example:6443/.well-known/openid-configuration"));
        assertTrue(config.isPublicOidcEndpoint("https://api.example:6443/openid/v1/jwks"));
        assertFalse(config.isPublicOidcEndpoint("https://api.example:6443/api/v1/namespaces/default"));
    }

    @Test
    void isKubernetesIssuerDetectsDefaultHosts() {
        KubeLocalDevConfig config = configForServer("https://api.example:6443");

        assertTrue(config.isKubernetesIssuer("https://kubernetes.default.svc"));
        assertTrue(config.isKubernetesIssuer("https://kubernetes.default.svc.cluster.local/openid/v1/jwks"));
        assertFalse(config.isKubernetesIssuer("https://accounts.google.com"));
        assertFalse(config.isKubernetesIssuer(""));
    }

    @Test
    void resolveIssuerFallsBackToDefaultOnDiscoveryFailure() throws Exception {
        server.stop(0);
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });
        server.start();
        String failingUrl = "http://127.0.0.1:" + server.getAddress().getPort();

        KubeLocalDevConfig config = configForServer(failingUrl);

        assertEquals(KubeLocalDevConfig.DEFAULT_KUBERNETES_ISSUER, config.resolveIssuerClaimFromDiscovery());
    }

    @Test
    void isPublicOidcEndpointHandlesInvalidUrl() {
        KubeLocalDevConfig config = configForServer("https://api.example:6443");

        assertFalse(config.isPublicOidcEndpoint(""));
        assertTrue(config.isPublicOidcEndpoint("not-a-valid-uri:///.well-known/openid-configuration"));
    }

    @Test
    void apiServerUrlIsCachedPerInstance() {
        KubeLocalDevConfig config = configForServer(baseUrl);

        assertEquals(baseUrl, config.apiServerUrl());
        assertEquals(baseUrl, config.apiServerUrl());
    }

    private KubeLocalDevConfig configForServer(String serverUrl) {
        return new KubeLocalDevConfig(() -> KubeConfigCredentials.builder()
                .serverUrl(serverUrl)
                .userToken("kube-user-token")
                .insecureSkipTlsVerify(true)
                .build());
    }
}
