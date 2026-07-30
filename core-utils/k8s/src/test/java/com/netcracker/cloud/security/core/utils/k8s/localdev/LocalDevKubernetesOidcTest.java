package com.netcracker.cloud.security.core.utils.k8s.localdev;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.properties.SystemProperties;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SystemStubsExtension.class)
class LocalDevKubernetesOidcTest {

    @SystemStub
    private SystemProperties systemProperties;

    @SystemStub
    private EnvironmentVariables environmentVariables;

    @TempDir
    Path tempDir;

    private HttpServer server;
    private String baseUrl;
    private final AtomicReference<String> lastAuth = new AtomicReference<>();

    @BeforeEach
    void setUp() throws IOException {
        LocalDevKubernetesOidc.resetCache();
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
        LocalDevKubernetesOidc.resetCache();
        System.clearProperty(LocalDevMode.QUARKUS_PROFILE_PROPERTY);
    }

    @Test
    void resolveIssuerFromDiscoveryWithoutAuth() throws Exception {
        Path kubeConfig = tempDir.resolve("config");
        Files.writeString(kubeConfig, """
                apiVersion: v1
                kind: Config
                current-context: test-ctx
                contexts:
                  - name: test-ctx
                    context:
                      cluster: test-cluster
                      user: test-user
                clusters:
                  - name: test-cluster
                    cluster:
                      server: %s
                      insecure-skip-tls-verify: true
                users:
                  - name: test-user
                    user:
                      token: kube-user-token
                """.formatted(baseUrl));
        environmentVariables.set("KUBECONFIG", kubeConfig.toString());
        systemProperties.set(LocalDevMode.QUARKUS_PROFILE_PROPERTY, "dev");

        assertEquals("https://kubernetes.default.svc", LocalDevKubernetesOidc.resolveIssuerClaimFromDiscovery());
        assertEquals(null, lastAuth.get());
        assertEquals(baseUrl + LocalDevKubernetesOidc.JWKS_PATH, LocalDevKubernetesOidc.jwksUrl());
        assertEquals("kube-user-token", LocalDevKubernetesOidc.userToken());
    }

    @Test
    void isPublicOidcEndpointDetectsDiscoveryAndJwks() {
        assertTrue(LocalDevKubernetesOidc.isPublicOidcEndpoint("https://api.example:6443/.well-known/openid-configuration"));
        assertTrue(LocalDevKubernetesOidc.isPublicOidcEndpoint("https://api.example:6443/openid/v1/jwks"));
        assertFalse(LocalDevKubernetesOidc.isPublicOidcEndpoint("https://api.example:6443/api/v1/namespaces/default"));
    }

    @Test
    void isKubernetesIssuerDetectsDefaultHosts() {
        assertTrue(LocalDevKubernetesOidc.isKubernetesIssuer("https://kubernetes.default.svc"));
        assertTrue(LocalDevKubernetesOidc.isKubernetesIssuer("https://kubernetes.default.svc.cluster.local/openid/v1/jwks"));
        assertFalse(LocalDevKubernetesOidc.isKubernetesIssuer("https://accounts.google.com"));
        assertFalse(LocalDevKubernetesOidc.isKubernetesIssuer(""));
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

        Path kubeConfig = tempDir.resolve("config");
        Files.writeString(kubeConfig, """
                apiVersion: v1
                kind: Config
                current-context: test-ctx
                contexts:
                  - name: test-ctx
                    context:
                      cluster: test-cluster
                      user: test-user
                clusters:
                  - name: test-cluster
                    cluster:
                      server: %s
                      insecure-skip-tls-verify: true
                users:
                  - name: test-user
                    user:
                      token: kube-user-token
                """.formatted(failingUrl));
        environmentVariables.set("KUBECONFIG", kubeConfig.toString());
        systemProperties.set(LocalDevMode.QUARKUS_PROFILE_PROPERTY, "dev");

        assertEquals(LocalDevKubernetesOidc.DEFAULT_KUBERNETES_ISSUER,
                LocalDevKubernetesOidc.resolveIssuerClaimFromDiscovery());
    }

    @Test
    void isPublicOidcEndpointHandlesInvalidUrl() {
        assertFalse(LocalDevKubernetesOidc.isPublicOidcEndpoint(""));
        assertTrue(LocalDevKubernetesOidc.isPublicOidcEndpoint("not-a-valid-uri:///.well-known/openid-configuration"));
    }

    @Test
    void apiServerUrlIsCachedUntilReset() throws Exception {
        Path kubeConfig = tempDir.resolve("config");
        Files.writeString(kubeConfig, """
                apiVersion: v1
                kind: Config
                current-context: test-ctx
                contexts:
                  - name: test-ctx
                    context:
                      cluster: test-cluster
                      user: test-user
                clusters:
                  - name: test-cluster
                    cluster:
                      server: %s
                      insecure-skip-tls-verify: true
                users:
                  - name: test-user
                    user:
                      token: kube-user-token
                """.formatted(baseUrl));
        environmentVariables.set("KUBECONFIG", kubeConfig.toString());

        assertEquals(baseUrl, LocalDevKubernetesOidc.apiServerUrl());
        assertEquals(baseUrl, LocalDevKubernetesOidc.apiServerUrl());
        LocalDevKubernetesOidc.resetCache();
        assertEquals(baseUrl, LocalDevKubernetesOidc.apiServerUrl());
    }
}
