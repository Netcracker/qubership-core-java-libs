package com.netcracker.cloud.security.core.utils.k8s.localdev;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SystemStubsExtension.class)
class KubeConfigLoaderTest {

    @SystemStub
    private EnvironmentVariables environmentVariables;

    @TempDir
    Path tempDir;

    @Test
    void loadsTokenAndCaFromKubeConfig() throws Exception {
        String ca = Base64.getEncoder().encodeToString("dummy-ca".getBytes(StandardCharsets.UTF_8));
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
                      server: https://127.0.0.1:6443
                      certificate-authority-data: %s
                users:
                  - name: test-user
                    user:
                      token: user-token-123
                """.formatted(ca));

        environmentVariables.set("KUBECONFIG", kubeConfig.toString());

        KubeConfigCredentials credentials = KubeConfigLoader.load();
        assertEquals("https://127.0.0.1:6443", credentials.getServerUrl());
        assertEquals("user-token-123", credentials.getUserToken());
        assertNotNull(credentials.getCertificateAuthorityData());
        assertFalse(credentials.isInsecureSkipTlsVerify());
    }

    @Test
    void loadsIdTokenFromOidcAuthProviderWhenTokenNotExpiredJwt() throws Exception {
        // non-JWT cached token cannot be parsed for exp → treated as expired,
        // but without refresh-token fields loader falls back to cached id-token
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
                      server: https://127.0.0.1:6443
                      insecure-skip-tls-verify: true
                users:
                  - name: test-user
                    user:
                      auth-provider:
                        name: oidc
                        config:
                          client-id: oauth-client
                          client-secret: oauth-secret
                          id-token: oidc-id-token-123
                """);

        environmentVariables.set("KUBECONFIG", kubeConfig.toString());

        KubeConfigCredentials credentials = KubeConfigLoader.load();
        assertEquals("oidc-id-token-123", credentials.getUserToken());
    }
}
