package com.netcracker.cloud.podsecrets.spring;

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
        System.clearProperty("pod.secrets.dir");
        System.clearProperty("pod.secrets.enabled");
    }

    @Test
    void postProcessEnvironment_addsSourceBeforeSystemEnvironment() throws Exception {
        Files.writeString(dir.resolve("db_password"), "secret");
        System.setProperty("pod.secrets.dir", dir.toString());

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
        System.setProperty("pod.secrets.dir", dir.toString());

        StandardEnvironment env = new StandardEnvironment();
        // Simulate env-var being present with a different value via system props
        // (StandardEnvironment reads System.getenv which we can't override easily;
        //  instead verify the source order gives precedence to pod-secrets)
        new PodSecretsEnvironmentPostProcessor()
                .postProcessEnvironment(env, Mockito.mock(SpringApplication.class));

        assertThat(env.getProperty("db_password")).isEqualTo("from-file");
        assertThat(env.getProperty("DB_PASSWORD")).isEqualTo("from-file");
        assertThat(env.getProperty("db.password")).isEqualTo("from-file");
    }

    @Test
    void postProcessEnvironment_disabledByProperty() throws Exception {
        Files.writeString(dir.resolve("k"), "v");
        System.setProperty("pod.secrets.dir", dir.toString());
        System.setProperty("pod.secrets.enabled", "false");

        StandardEnvironment env = new StandardEnvironment();
        new PodSecretsEnvironmentPostProcessor()
                .postProcessEnvironment(env, Mockito.mock(SpringApplication.class));

        assertThat(env.getPropertySources().contains(PodSecretsPropertySource.SOURCE_NAME)).isFalse();
    }

    @Test
    void postProcessEnvironment_missingDir_noSourceAdded() {
        System.setProperty("pod.secrets.dir", dir.resolve("nonexistent").toString());

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
