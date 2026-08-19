package com.netcracker.cloud.security.core.utils.k8s.localdev.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KubeConfigLoaderTest {

    @TempDir
    Path tempDir;

    private KubeConfigLoader loaderForKubeConfigEnv(String kubeConfigEnv) {
        return new KubeConfigLoader(kubeConfigEnv, System.getProperty("user.home"));
    }

    @Test
    void loadsCertificateAuthorityFromFile() throws Exception {
        Path caFile = tempDir.resolve("cluster-ca.crt");
        Files.writeString(caFile, "dummy-ca-from-file");

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
                      certificate-authority: cluster-ca.crt
                users:
                  - name: test-user
                    user:
                      token: user-token-123
                """);

        KubeConfigCredentials credentials = loaderForKubeConfigEnv(kubeConfig.toString()).load();
        assertEquals("dummy-ca-from-file", new String(credentials.getCertificateAuthorityData(), StandardCharsets.UTF_8));
    }

    @Test
    void prefersCertificateAuthorityDataOverFile() throws Exception {
        Path caFile = tempDir.resolve("cluster-ca.crt");
        Files.writeString(caFile, "from-file");

        String caData = Base64.getEncoder().encodeToString("from-data".getBytes(StandardCharsets.UTF_8));
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
                      certificate-authority: cluster-ca.crt
                      certificate-authority-data: %s
                users:
                  - name: test-user
                    user:
                      token: user-token-123
                """.formatted(caData));

        KubeConfigCredentials credentials = loaderForKubeConfigEnv(kubeConfig.toString()).load();
        assertEquals("from-data", new String(credentials.getCertificateAuthorityData(), StandardCharsets.UTF_8));
    }

    @Test
    void failsWhenCertificateAuthorityFileMissing() throws Exception {
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
                      certificate-authority: missing-ca.crt
                users:
                  - name: test-user
                    user:
                      token: user-token-123
                """);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                loaderForKubeConfigEnv(kubeConfig.toString())::load);
        assertTrue(ex.getMessage().contains("certificate-authority"));
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

    @ParameterizedTest
    @MethodSource("userTokenKubeConfigs")
    void loadsUserTokenFromKubeConfig(String usersYaml, String expectedToken) throws Exception {
        Path kubeConfig = tempDir.resolve("config");
        Files.writeString(kubeConfig, kubeConfigWithUsers(usersYaml));
        assertEquals(expectedToken, loaderForKubeConfigEnv(kubeConfig.toString()).load().getUserToken());
    }

    static Stream<Arguments> userTokenKubeConfigs() {
        return Stream.of(
                Arguments.of("""
                                  - name: test-user
                                    user:
                                      access-token: direct-access-token
                        """, "direct-access-token"),
                Arguments.of("""
                                  - name: test-user
                                    user:
                                      auth-provider:
                                        name: gcp
                                        config:
                                          access-token: provider-access-token
                        """, "provider-access-token"),
                Arguments.of("""
                                  - name: test-user
                                    user:
                                      auth-provider:
                                        name: oidc
                                        config:
                                          client-id: oauth-client
                                          client-secret: oauth-secret
                                          id-token: oidc-id-token-123
                        """, "oidc-id-token-123"));
    }

    @Test
    void rejectsExecAuthentication() throws Exception {
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
                        command: kubectl
                        args:
                          - oidc-login
                          - get-token
                """);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                loaderForKubeConfigEnv(kubeConfig.toString())::load);
        assertTrue(ex.getMessage().contains("exec"));
        assertTrue(ex.getMessage().contains("not supported"));
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

    @ParameterizedTest
    @MethodSource("invalidKubeConfigs")
    void failsWhenKubeconfigIsInvalid(String yaml) throws Exception {
        Path kubeConfig = tempDir.resolve("config");
        Files.writeString(kubeConfig, yaml);
        assertThrows(IllegalStateException.class, loaderForKubeConfigEnv(kubeConfig.toString())::load);
    }

    static Stream<String> invalidKubeConfigs() {
        return Stream.of(
                """
                        apiVersion: v1
                        kind: Config
                        contexts: []
                        clusters: []
                        users: []
                        """,
                """
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
                        """,
                """
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
                        """,
                """
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
                        """,
                "{ not valid yaml [[[",
                """
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
    }

    private static String kubeConfigWithUsers(String usersYaml) {
        return """
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
                """ + usersYaml;
    }

    @Test
    void resolveKubeConfigPathUsesDefaultWhenUnset() {
        KubeConfigLoader loader = new KubeConfigLoader(null, System.getProperty("user.home"));
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
}
