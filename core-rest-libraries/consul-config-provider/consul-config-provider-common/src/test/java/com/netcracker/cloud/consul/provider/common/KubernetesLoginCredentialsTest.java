package com.netcracker.cloud.consul.provider.common;

import com.netcracker.cloud.security.core.utils.k8s.AudienceName;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KubernetesLoginCredentialsTest {

    private static final String TOKENS_DIR_PROP = "com.netcracker.cloud.security.kubernetes.tokens.dir";
    private static final String POLLING_INTERVAL_PROP = "com.netcracker.cloud.security.kubernetes.tokens.polling.interval";

    @TempDir
    static Path tokensDir;

    @BeforeAll
    static void pointTokenSourceToTempDir() {
        System.setProperty(TOKENS_DIR_PROP, tokensDir.toString());
        System.setProperty(POLLING_INTERVAL_PROP, "PT0S");
    }

    private static void writeToken(String audience, String value) throws IOException {
        Path audienceDir = Files.createDirectories(tokensDir.resolve(audience));
        Files.writeString(audienceDir.resolve("token"), value);
    }

    @Test
    void authMethodIsTheGivenName() {
        KubernetesLoginCredentials credentials = new KubernetesLoginCredentials("core-k8s", AudienceName.NETCRACKER);
        assertEquals("core-k8s", credentials.getAuthMethod());
    }

    @Test
    void bearerTokenIsReadOnEveryCallAndNotCached() throws IOException {
        writeToken(AudienceName.NETCRACKER, "first-projected-token");
        KubernetesLoginCredentials credentials = new KubernetesLoginCredentials("core-k8s", AudienceName.NETCRACKER);

        assertEquals("first-projected-token", credentials.getBearerToken());

        writeToken(AudienceName.NETCRACKER, "rotated-projected-token");
        assertEquals("rotated-projected-token", credentials.getBearerToken());
    }

    @Test
    void bearerTokenFailsWithRuntimeExceptionWhenTokenFileIsUnreadable() throws IOException {
        Files.createDirectories(tokensDir.resolve(AudienceName.MAAS).resolve("token"));
        KubernetesLoginCredentials credentials = new KubernetesLoginCredentials("core-k8s", AudienceName.MAAS);

        RuntimeException thrown = assertThrows(RuntimeException.class, credentials::getBearerToken);
        assertInstanceOf(IOException.class, thrown.getCause());
    }

    @Test
    void bearerTokenFailsWithIllegalArgumentExceptionForUnknownAudience() {
        KubernetesLoginCredentials credentials = new KubernetesLoginCredentials("core-k8s", "no-such-audience");

        assertThrows(IllegalArgumentException.class, credentials::getBearerToken);
    }
}
