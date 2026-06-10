package com.netcracker.cloud.podsecrets.spring;

import com.netcracker.cloud.podsecrets.PodSecretsLoader;
import com.netcracker.cloud.podsecrets.PodSecretsLoaderConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class PodSecretsPropertySourceTest {

    @TempDir
    Path dir;

    private PodSecretsPropertySource build() {
        PodSecretsLoader loader = new PodSecretsLoader(PodSecretsLoaderConfig.of(dir, Duration.ofMinutes(10)));
        return new PodSecretsPropertySource(loader);
    }

    @Test
    void getName_isConstant() {
        assertThat(build().getName()).isEqualTo(PodSecretsPropertySource.SOURCE_NAME);
    }

    @Test
    void getProperty_allThreeForms() throws Exception {
        Files.writeString(dir.resolve("db_password"), "secret");
        PodSecretsPropertySource ps = build();

        assertThat(ps.getProperty("DB_PASswORD")).isEqualTo("secret");
    }

    @Test
    void getPropertyNames_UpperCaseANDLowerCase() throws Exception {
        Files.writeString(dir.resolve("api_token"), "tok");
        PodSecretsPropertySource ps = build();

        assertThat(ps.getPropertyNames()).contains("api_token", "API_TOKEN");
    }

    @Test
    void getProperty_missingKey_returnsNull() throws Exception {
        Files.writeString(dir.resolve("k"), "v");
        assertThat(build().getProperty("nonexistent")).isNull();
    }
}
