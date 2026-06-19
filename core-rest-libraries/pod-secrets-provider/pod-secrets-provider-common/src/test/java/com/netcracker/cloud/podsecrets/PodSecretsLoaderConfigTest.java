package com.netcracker.cloud.podsecrets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;

import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class PodSecretsLoaderConfigTest {

    @Test
    void fromSystem_defaults() {
        PodSecretsLoaderConfig cfg = PodSecretsLoaderConfig.fromSystem();
        assertThat(cfg.getBaseDir()).isEqualTo(PodSecretsLoaderConfig.DEFAULT_BASE_DIR);
        assertThat(cfg.getTtl()).isEqualTo(PodSecretsLoaderConfig.DEFAULT_TTL);
    }

    @Test
    void fromSystem_systemPropertyOverridesDir(@TempDir Path tmp) {
        withProperty(PodSecretsLoaderConfig.PROP_POD_SECRETS_DIR, tmp.toString(), () -> {
            PodSecretsLoaderConfig cfg = PodSecretsLoaderConfig.fromSystem();
            assertThat(cfg.getBaseDir()).isEqualTo(tmp);
        });
    }

    @Test
    void fromSystem_ttlOverride() {
        withProperty(PodSecretsLoaderConfig.PROP_POD_SECRETS_TTL, "PT30S", () -> {
            PodSecretsLoaderConfig cfg = PodSecretsLoaderConfig.fromSystem();
            assertThat(cfg.getTtl()).isEqualTo(Duration.ofSeconds(30));
        });
    }

    @Test
    void fromSystem_envVarOverridesDir(@TempDir Path tmp) throws Exception {
        new EnvironmentVariables(PodSecretsLoaderConfig.ENV_PROP_POD_SECRETS_DIR, tmp.toString()).execute(() -> {
            PodSecretsLoaderConfig cfg = PodSecretsLoaderConfig.fromSystem();
            assertThat(cfg.getBaseDir()).isEqualTo(tmp);
        });
    }

    private void withProperty(String propName, String propValue, Runnable runnable) {
        var prevValue = System.getProperty(propName);
        System.setProperty(propName, propValue);
        try {
            runnable.run();
        } finally {
            if (prevValue != null) {
                System.setProperty(propName, prevValue);
            } else {
                System.clearProperty(propName);
            }
        }
    }
}
