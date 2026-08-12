package com.netcracker.cloud.security.core.utils.k8s.localdev.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KubeConfigLoaderTest {

    @TempDir
    Path tempDir;

    private KubeConfigLoader loaderForKubeConfigEnv(String kubeConfigEnv) {
        return new KubeConfigLoader(() -> kubeConfigEnv, () -> System.getProperty("user.home"));
    }

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

        KubeConfigCredentials credentials = loaderForKubeConfigEnv(kubeConfig.toString()).load();
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

        KubeConfigCredentials credentials = loaderForKubeConfigEnv(kubeConfig.toString()).load();
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

        assertEquals("direct-access-token", loaderForKubeConfigEnv(kubeConfig.toString()).load().getUserToken());
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

        assertEquals("provider-access-token", loaderForKubeConfigEnv(kubeConfig.toString()).load().getUserToken());
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
            execArg = "/c type " + tokenJson;
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

        assertEquals("exec-token", loaderForKubeConfigEnv(kubeConfig.toString()).load().getUserToken());
    }

    @Test
    void resolveKubeConfigPathUsesFirstEntryFromList() {
        Path first = tempDir.resolve("first-config");
        Path second = tempDir.resolve("second-config");
        String kubeConfigList = first + java.io.File.pathSeparator + second;
        assertEquals(first, loaderForKubeConfigEnv(kubeConfigList).resolveKubeConfigPath());
    }

    @Test
    void failsWhenKubeconfigMissing() {
        assertThrows(IllegalStateException.class,
                loaderForKubeConfigEnv(tempDir.resolve("missing").toString())::load);
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
        assertThrows(IllegalStateException.class, loaderForKubeConfigEnv(kubeConfig.toString())::load);
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
        assertThrows(IllegalStateException.class, loaderForKubeConfigEnv(kubeConfig.toString())::load);
    }

    @Test
    void resolveKubeConfigPathUsesDefaultWhenUnset() {
        KubeConfigLoader loader = new KubeConfigLoader(() -> null, () -> System.getProperty("user.home"));
        assertEquals(Path.of(System.getProperty("user.home"), ".kube", "config"), loader.resolveKubeConfigPath());
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
        assertEquals("https://127.0.0.1:6443", loaderForKubeConfigEnv(kubeConfig.toString()).load().getServerUrl());
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
        assertThrows(IllegalStateException.class, loaderForKubeConfigEnv(kubeConfig.toString())::load);
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
        assertThrows(IllegalStateException.class, loaderForKubeConfigEnv(kubeConfig.toString())::load);
    }

    @Test
    void failsWhenKubeconfigIsInvalid() throws Exception {
        Path kubeConfig = tempDir.resolve("config");
        Files.writeString(kubeConfig, "{ not valid yaml [[[");
        assertThrows(IllegalStateException.class, loaderForKubeConfigEnv(kubeConfig.toString())::load);
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
        assertThrows(IllegalStateException.class, loaderForKubeConfigEnv(kubeConfig.toString())::load);
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
        assertEquals("direct-id-token", loaderForKubeConfigEnv(kubeConfig.toString()).load().getUserToken());
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
        assertThrows(IllegalStateException.class, loaderForKubeConfigEnv(kubeConfig.toString())::load);
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
        assertThrows(IllegalStateException.class, loaderForKubeConfigEnv(kubeConfig.toString())::load);
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
        assertThrows(IllegalStateException.class, loaderForKubeConfigEnv(kubeConfig.toString())::load);
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
            execArg = "/c type " + tokenJson;
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

        assertEquals("env-exec-token", loaderForKubeConfigEnv(kubeConfig.toString()).load().getUserToken());
    }
}
