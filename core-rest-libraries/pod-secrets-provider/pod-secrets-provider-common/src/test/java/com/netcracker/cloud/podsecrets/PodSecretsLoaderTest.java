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
    void getAbsentSecrets() {
        PodSecretsLoader l = new PodSecretsLoader(PodSecretsLoaderConfig.of(Path.of("/absent"), Duration.ofMinutes(10)));
        assertEquals(0, l.getSecrets().size());
    }

    @Test
    @SuppressWarnings("java:S2925")
    void storedValueReturnedWhenFileUnreadable() throws Exception {
        Path fileA = dir.resolve("db_password");
        Path fileB = dir.resolve("api_token");
        Files.writeString(fileA, "secret-pass");
        Files.writeString(fileB, "secret-token");

        PodSecretsLoader loader = new PodSecretsLoader(
                PodSecretsLoaderConfig.of(dir, Duration.ofMillis(1)));

        // first load — both files readable
        assertThat(loader.getSecrets())
                .containsEntry("db.password", "secret-pass")
                .containsEntry("api.token", "secret-token");

        // replace db_password file with a directory — Files.readString() throws IOException
        Files.delete(fileA);
        Files.createDirectory(fileA);

        Thread.sleep(2); // wait for TTL to expire

        assertThat(loader.getSecrets())
                .containsEntry("db.password", "secret-pass")
                .containsEntry("api.token", "secret-token");
    }
}
