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

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.netcracker.cloud.security.core.utils.k8s.localdev.impl.KubeLocalDevConfig.JWKS_PATH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SystemStubsExtension.class)
class LocalDevKubernetesOidcTest {

    @SystemStub
    private EnvironmentVariables environmentVariables;

    @TempDir
    Path tempDir;

    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/.well-known/openid-configuration", exchange -> {
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
    void exposesJwksUrlAndIssuerDiscoveryThroughFacade() throws Exception {
        writeKubeConfig(baseUrl);
        LocalDevKubernetesOidc oidc = new LocalDevKubernetesOidc();

        assertEquals("https://kubernetes.default.svc", oidc.resolveIssuerClaimFromDiscovery());
        assertEquals(baseUrl + JWKS_PATH, oidc.jwksUrl());
        assertTrue(oidc.isKubernetesIssuer("https://kubernetes.default.svc"));
        assertFalse(oidc.isKubernetesIssuer("https://accounts.google.com"));
    }

    private void writeKubeConfig(String serverUrl) throws IOException {
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
                """.formatted(serverUrl));
        environmentVariables.set("KUBECONFIG", kubeConfig.toString());
    }
}
