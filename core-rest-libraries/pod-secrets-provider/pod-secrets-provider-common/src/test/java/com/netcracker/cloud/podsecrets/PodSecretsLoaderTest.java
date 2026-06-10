package com.netcracker.cloud.podsecrets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PodSecretsLoaderTest {

    @TempDir
    Path dir;

    @Test
    void getSecretCaseinsensitive() throws Exception {
        Files.writeString(dir.resolve("db_password"), "secret123");
        PodSecretsLoader l = new PodSecretsLoader(PodSecretsLoaderConfig.of(dir, Duration.ofMinutes(10)));
        assertThat(l.getSecrets().get("DB_PASswORD")).isEqualTo("secret123");

        // assert that set of property names also case-insensitive on select
        assertTrue(l.getSecrets().keySet().contains("DB_PASswORD"));
    }

    @Test
    void getAbsentSecrets() throws Exception {
        PodSecretsLoader l = new PodSecretsLoader(PodSecretsLoaderConfig.of(Path.of("/absent"), Duration.ofMinutes(10)));
        assertEquals(0, l.getSecrets().size());
    }
}
