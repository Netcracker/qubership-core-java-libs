package com.netcracker.cloud.podsecrets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class PodSecretsLoaderConfigTest {

    @Test
    void fromSystem_defaults() {
        System.clearProperty("pod.secrets.dir");
        System.clearProperty("pod.secrets.ttl");

        PodSecretsLoaderConfig cfg = PodSecretsLoaderConfig.fromSystem();
        assertThat(cfg.getBaseDir()).isEqualTo(PodSecretsLoaderConfig.DEFAULT_BASE_DIR);
        assertThat(cfg.getTtl()).isEqualTo(PodSecretsLoaderConfig.DEFAULT_TTL);
    }

    @Test
    void fromSystem_systemPropertyOverridesDir(@TempDir Path tmp) {
        System.setProperty("pod.secrets.dir", tmp.toString());
        System.clearProperty("pod.secrets.ttl");
        try {
            PodSecretsLoaderConfig cfg = PodSecretsLoaderConfig.fromSystem();
            assertThat(cfg.getBaseDir()).isEqualTo(tmp);
        } finally {
            System.clearProperty("pod.secrets.dir");
        }
    }

    @Test
    void fromSystem_ttlOverride() {
        System.setProperty("pod.secrets.ttl", "PT30S");
        try {
            PodSecretsLoaderConfig cfg = PodSecretsLoaderConfig.fromSystem();
            assertThat(cfg.getTtl()).isEqualTo(Duration.ofSeconds(30));
        } finally {
            System.clearProperty("pod.secrets.ttl");
        }
    }
}
