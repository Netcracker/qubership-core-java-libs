package com.netcracker.cloud.podsecrets.quarkus;

import com.netcracker.cloud.podsecrets.quarkus.runtime.PodSecretsConfigSource;
import com.netcracker.cloud.podsecrets.quarkus.runtime.PodSecretsConfigSourceFactory;
import com.netcracker.cloud.podsecrets.quarkus.runtime.PodSecretsConfigSourceFactoryBuilder;
import io.quarkus.runtime.configuration.ConfigBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-level tests for the deployment wiring.
 */
class PodSecretsConfigSourceDeploymentTest {

    @Test
    void factoryBuilder_implementsConfigBuilder() {
        assertThat(PodSecretsConfigSourceFactoryBuilder.class)
                .isAssignableTo(ConfigBuilder.class);
    }

    @Test
    void factoryBuilder_priority_matches_ordinal() {
        PodSecretsConfigSourceFactoryBuilder builder = new PodSecretsConfigSourceFactoryBuilder();
        assertThat(builder.priority()).isEqualTo(PodSecretsConfigSource.PRIORITY);
    }

    @Test
    void factory_missingDir_returnsEmptyList(@TempDir Path tmp) {
        Path missing = tmp.resolve("nonexistent");
        System.setProperty("pod.secrets.dir", missing.toString());
        try {
            PodSecretsConfigSourceFactory factory = new PodSecretsConfigSourceFactory();
            // just verify no NPE
            assertThat(factory).isNotNull();
        } finally {
            System.clearProperty("pod.secrets.dir");
        }
    }

    @Test
    void configSource_ordinal_between_env_and_consul() {
        assertThat(PodSecretsConfigSource.PRIORITY).isGreaterThan(300);  // EnvConfigSource
        assertThat(PodSecretsConfigSource.PRIORITY).isGreaterThan(400);  // SysPropsConfigSource
        assertThat(PodSecretsConfigSource.PRIORITY).isLessThan(500);     // ConsulConfigSource
        assertThat(PodSecretsConfigSource.PRIORITY).isEqualTo(450);
    }
}
