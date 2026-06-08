package com.netcracker.cloud.podsecrets.quarkus.runtime;

import com.netcracker.cloud.podsecrets.PodSecretsLoader;
import com.netcracker.cloud.podsecrets.PodSecretsLoaderConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class PodSecretsConfigSourceUnitTest {

    @TempDir
    Path dir;

    private PodSecretsConfigSource buildSource() {
        PodSecretsLoader loader = new PodSecretsLoader(PodSecretsLoaderConfig.of(dir, Duration.ofMinutes(10)));
        return new PodSecretsConfigSource(loader);
    }

    @Test
    void getName() {
        assertThat(buildSource().getName()).isEqualTo("pod-secrets-config-source");
    }

    @Test
    void getOrdinal_is450() {
        assertThat(buildSource().getOrdinal()).isEqualTo(450);
    }

    @Test
    void ordinal_higherThanEnv_lowerThanConsul() {
        int ordinal = PodSecretsConfigSource.PRIORITY;
        assertThat(ordinal).isGreaterThan(300); // EnvConfigSource
        assertThat(ordinal).isGreaterThan(400); // SysPropsConfigSource
        assertThat(ordinal).isLessThan(500);    // ConsulConfigSource
    }

    @Test
    void getValue_allForms() throws Exception {
        Files.writeString(dir.resolve("db_password"), "secret");
        PodSecretsConfigSource source = buildSource();

        assertThat(source.getValue("db_password")).isEqualTo("secret");
        assertThat(source.getValue("DB_PASSWORD")).isEqualTo("secret");
        assertThat(source.getValue("db.password")).isEqualTo("secret");
    }

    @Test
    void getPropertyNames_containsAllForms() throws Exception {
        Files.writeString(dir.resolve("api_token"), "tok");
        assertThat(buildSource().getPropertyNames()).contains("api_token", "API_TOKEN", "api.token");
    }
}
