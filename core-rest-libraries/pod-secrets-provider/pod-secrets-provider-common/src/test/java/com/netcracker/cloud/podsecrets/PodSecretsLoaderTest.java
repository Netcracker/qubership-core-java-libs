package com.netcracker.cloud.podsecrets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PodSecretsLoaderTest {

    @TempDir
    Path dir;

    @Test
    void getSecretCaseinsensitive() throws Exception {
        Files.writeString(dir.resolve("db_password"), "secret123");
        PodSecretsLoader l = new PodSecretsLoader(PodSecretsLoaderConfig.of(dir, Duration.ofMinutes(10)));
        assertThat(l.getSecrets())
                .containsEntry("DB_PASSWORD", "secret123")
                .containsEntry("db_password", "secret123")
                .containsEntry("db.password", "secret123");
    }

    @Test
    void getAbsentSecrets() throws Exception {
        PodSecretsLoader l = new PodSecretsLoader(PodSecretsLoaderConfig.of(Path.of("/absent"), Duration.ofMinutes(10)));
        assertEquals(0, l.getSecrets().size());
    }
}
