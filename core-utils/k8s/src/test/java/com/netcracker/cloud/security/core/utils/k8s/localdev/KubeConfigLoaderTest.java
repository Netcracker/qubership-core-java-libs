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
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    @Test
    void loadsAccessTokenFromUser() throws Exception {
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
                users:
                  - name: test-user
                    user:
                      access-token: direct-access-token
                """);
        environmentVariables.set("KUBECONFIG", kubeConfig.toString());

        assertEquals("direct-access-token", KubeConfigLoader.load().getUserToken());
    }

    @Test
    void loadsAccessTokenFromNonOidcAuthProvider() throws Exception {
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
                users:
                  - name: test-user
                    user:
                      auth-provider:
                        name: gcp
                        config:
                          access-token: provider-access-token
                """);
        environmentVariables.set("KUBECONFIG", kubeConfig.toString());

        assertEquals("provider-access-token", KubeConfigLoader.load().getUserToken());
    }

    @Test
    void loadsTokenViaExecCredential() throws Exception {
        Path tokenJson = tempDir.resolve("token.json");
        Files.writeString(tokenJson, "{\"status\":{\"token\":\"exec-token\"}}");

        Path kubeConfig = tempDir.resolve("config");
        String execCommand;
        String execArg;
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            execCommand = "cmd";
            execArg = "/c type " + tokenJson.toString();
        } else {
            execCommand = "cat";
            execArg = tokenJson.toString();
        }
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
                users:
                  - name: test-user
                    user:
                      exec:
                        command: %s
                        args:
                          - %s
                """.formatted(execCommand, execArg));
        environmentVariables.set("KUBECONFIG", kubeConfig.toString());

        assertEquals("exec-token", KubeConfigLoader.load().getUserToken());
    }

    @Test
    void resolveKubeConfigPathUsesFirstEntryFromList() {
        Path first = tempDir.resolve("first-config");
        Path second = tempDir.resolve("second-config");
        environmentVariables.set("KUBECONFIG", first + java.io.File.pathSeparator + second);
        assertEquals(first, KubeConfigLoader.resolveKubeConfigPath());
    }

    @Test
    void failsWhenKubeconfigMissing() {
        environmentVariables.set("KUBECONFIG", tempDir.resolve("missing").toString());
        assertThrows(IllegalStateException.class, KubeConfigLoader::load);
    }

    @Test
    void failsWhenCurrentContextMissing() throws Exception {
        Path kubeConfig = tempDir.resolve("config");
        Files.writeString(kubeConfig, """
                apiVersion: v1
                kind: Config
                contexts: []
                clusters: []
                users: []
                """);
        environmentVariables.set("KUBECONFIG", kubeConfig.toString());
        assertThrows(IllegalStateException.class, KubeConfigLoader::load);
    }

    @Test
    void failsWhenClusterHasNoServer() throws Exception {
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
                    cluster: {}
                users:
                  - name: test-user
                    user:
                      token: token
                """);
        environmentVariables.set("KUBECONFIG", kubeConfig.toString());
        assertThrows(IllegalStateException.class, KubeConfigLoader::load);
    }

    @Test
    void resolveKubeConfigPathUsesDefaultWhenUnset() {
        environmentVariables.remove("KUBECONFIG");
        assertEquals(Path.of(System.getProperty("user.home"), ".kube", "config"),
                KubeConfigLoader.resolveKubeConfigPath());
    }

    @Test
    void stripsTrailingSlashFromServerUrl() throws Exception {
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
                      server: https://127.0.0.1:6443/
                users:
                  - name: test-user
                    user:
                      token: token
                """);
        environmentVariables.set("KUBECONFIG", kubeConfig.toString());
        assertEquals("https://127.0.0.1:6443", KubeConfigLoader.load().getServerUrl());
    }

    @Test
    void failsWhenContextMissingClusterOrUser() throws Exception {
        Path kubeConfig = tempDir.resolve("config");
        Files.writeString(kubeConfig, """
                apiVersion: v1
                kind: Config
                current-context: test-ctx
                contexts:
                  - name: test-ctx
                    context:
                      user: test-user
                clusters:
                  - name: test-cluster
                    cluster:
                      server: https://127.0.0.1:6443
                users:
                  - name: test-user
                    user:
                      token: token
                """);
        environmentVariables.set("KUBECONFIG", kubeConfig.toString());
        assertThrows(IllegalStateException.class, KubeConfigLoader::load);
    }

    @Test
    void failsWhenKubeconfigEntryNotFound() throws Exception {
        Path kubeConfig = tempDir.resolve("config");
        Files.writeString(kubeConfig, """
                apiVersion: v1
                kind: Config
                current-context: test-ctx
                contexts:
                  - name: test-ctx
                    context:
                      cluster: missing-cluster
                      user: test-user
                clusters:
                  - name: test-cluster
                    cluster:
                      server: https://127.0.0.1:6443
                users:
                  - name: test-user
                    user:
                      token: token
                """);
        environmentVariables.set("KUBECONFIG", kubeConfig.toString());
        assertThrows(IllegalStateException.class, KubeConfigLoader::load);
    }

    @Test
    void failsWhenKubeconfigIsInvalid() throws Exception {
        Path kubeConfig = tempDir.resolve("config");
        Files.writeString(kubeConfig, "{ not valid yaml [[[");
        environmentVariables.set("KUBECONFIG", kubeConfig.toString());
        assertThrows(IllegalStateException.class, KubeConfigLoader::load);
    }

    @Test
    void failsWhenUserHasNoCredentials() throws Exception {
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
                users:
                  - name: test-user
                    user: {}
                """);
        environmentVariables.set("KUBECONFIG", kubeConfig.toString());
        assertThrows(IllegalStateException.class, KubeConfigLoader::load);
    }

    @Test
    void skipsAuthProviderWhenConfigMissing() throws Exception {
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
                users:
                  - name: test-user
                    user:
                      auth-provider:
                        name: oidc
                      id-token: direct-id-token
                """);
        environmentVariables.set("KUBECONFIG", kubeConfig.toString());
        assertEquals("direct-id-token", KubeConfigLoader.load().getUserToken());
    }

    @Test
    void failsWhenExecCommandEmpty() throws Exception {
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
                users:
                  - name: test-user
                    user:
                      exec:
                        command: ""
                """);
        environmentVariables.set("KUBECONFIG", kubeConfig.toString());
        assertThrows(IllegalStateException.class, KubeConfigLoader::load);
    }

    @Test
    void failsWhenExecReturnsNonZeroExitCode() throws Exception {
        Path kubeConfig = tempDir.resolve("config");
        String execCommand = System.getProperty("os.name").toLowerCase().contains("win") ? "cmd" : "sh";
        String execArg = System.getProperty("os.name").toLowerCase().contains("win") ? "/c exit 1" : "-c exit 1";
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
                users:
                  - name: test-user
                    user:
                      exec:
                        command: %s
                        args:
                          - %s
                """.formatted(execCommand, execArg));
        environmentVariables.set("KUBECONFIG", kubeConfig.toString());
        assertThrows(IllegalStateException.class, KubeConfigLoader::load);
    }

    @Test
    void failsWhenExecReturnsNoToken() throws Exception {
        Path kubeConfig = tempDir.resolve("config");
        String execCommand = System.getProperty("os.name").toLowerCase().contains("win") ? "cmd" : "echo";
        String execArg = System.getProperty("os.name").toLowerCase().contains("win")
                ? "/c echo {}"
                : "{}";
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
                users:
                  - name: test-user
                    user:
                      exec:
                        command: %s
                        args:
                          - %s
                """.formatted(execCommand, execArg));
        environmentVariables.set("KUBECONFIG", kubeConfig.toString());
        assertThrows(IllegalStateException.class, KubeConfigLoader::load);
    }

    @Test
    void loadsTokenViaExecWithEnvVars() throws Exception {
        Path tokenJson = tempDir.resolve("token.json");
        Files.writeString(tokenJson, "{\"status\":{\"token\":\"env-exec-token\"}}");

        Path kubeConfig = tempDir.resolve("config");
        String execCommand;
        String execArg;
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            execCommand = "cmd";
            execArg = "/c type " + tokenJson.toString();
        } else {
            execCommand = "cat";
            execArg = tokenJson.toString();
        }
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
                users:
                  - name: test-user
                    user:
                      exec:
                        command: %s
                        args:
                          - %s
                        env:
                          - name: LOCAL_DEV_TEST
                            value: marker
                          - name: EMPTY_VALUE
                """.formatted(execCommand, execArg));
        environmentVariables.set("KUBECONFIG", kubeConfig.toString());

        assertEquals("env-exec-token", KubeConfigLoader.load().getUserToken());
    }
}
