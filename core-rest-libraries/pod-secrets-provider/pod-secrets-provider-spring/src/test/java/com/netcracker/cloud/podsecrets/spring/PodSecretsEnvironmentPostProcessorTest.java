package com.netcracker.cloud.podsecrets.spring;

import com.netcracker.cloud.podsecrets.PodSecretsLoaderConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PodSecretsEnvironmentPostProcessorTest {

    @TempDir
    Path dir;

    @AfterEach
    void clearProps() {
        System.clearProperty(PodSecretsLoaderConfig.PROP_POD_SECRETS_DIR);
        System.clearProperty(PodSecretsLoaderConfig.PROP_POD_SECRETS_ENABLED);
    }

    @Test
    void postProcessEnvironment_addsSourceBeforeSystemEnvironment() throws Exception {
        Files.writeString(dir.resolve("db_password"), "secret");
        System.setProperty(PodSecretsLoaderConfig.PROP_POD_SECRETS_DIR, dir.toString());

        StandardEnvironment env = new StandardEnvironment();
        new PodSecretsEnvironmentPostProcessor()
                .postProcessEnvironment(env, Mockito.mock(SpringApplication.class));

        MutablePropertySources sources = env.getPropertySources();
        assertThat(sources.contains(PodSecretsPropertySource.SOURCE_NAME)).isTrue();

        // pod-secrets must be positioned BEFORE systemEnvironment
        int podIdx = indexOf(sources, PodSecretsPropertySource.SOURCE_NAME);
        int envIdx = indexOf(sources, StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        assertThat(podIdx).isLessThan(envIdx);
    }

    @Test
    void postProcessEnvironment_secretOverridesEnvVar() throws Exception {
        Files.writeString(dir.resolve("db_password"), "from-file");
        System.setProperty(PodSecretsLoaderConfig.PROP_POD_SECRETS_DIR, dir.toString());

        StandardEnvironment env = new StandardEnvironment();
        new PodSecretsEnvironmentPostProcessor()
                .postProcessEnvironment(env, Mockito.mock(SpringApplication.class));

        assertThat(env.getProperty("DB_PASSWORD")).isEqualTo("from-file");
    }

    @Test
    void postProcessEnvironment_disabledByProperty() throws Exception {
        Files.writeString(dir.resolve("k"), "v");
        System.setProperty(PodSecretsLoaderConfig.PROP_POD_SECRETS_DIR, dir.toString());
        System.setProperty(PodSecretsLoaderConfig.PROP_POD_SECRETS_ENABLED, "false");

        StandardEnvironment env = new StandardEnvironment();
        new PodSecretsEnvironmentPostProcessor()
                .postProcessEnvironment(env, Mockito.mock(SpringApplication.class));

        assertThat(env.getPropertySources().contains(PodSecretsPropertySource.SOURCE_NAME)).isFalse();
    }

    private static int indexOf(MutablePropertySources sources, String name) {
        int i = 0;
        for (org.springframework.core.env.PropertySource<?> s : sources) {
            if (s.getName().equals(name)) return i;
            i++;
        }
        return -1;
    }
}
