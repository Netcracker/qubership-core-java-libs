package com.netcracker.cloud.podsecrets.spring;

import com.netcracker.cloud.podsecrets.PodSecretsLoaderConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link PodSecretsEnvironmentPostProcessor} is <b>auto-discovered</b> by Spring Boot
 * during application bootstrap, i.e. that it is declared where Spring Boot actually looks for
 * {@code EnvironmentPostProcessor}s: {@code META-INF/spring.factories}.
 *
 * <p>{@link PodSecretsEnvironmentPostProcessorTest} invokes the processor directly, so it verifies
 * the logic but cannot catch a broken registration. This test boots a real {@link SpringApplication}
 * and asserts the pod-secrets property source ends up in the environment. It guards against the
 * regression where the processor was declared only in a {@code META-INF/spring/*.imports} file,
 * which Spring Boot does not read for {@code EnvironmentPostProcessor} — leaving secrets silently
 * unread and callers falling back to property defaults.
 */
class PodSecretsEnvironmentPostProcessorAutoRegistrationTest {

    @TempDir
    Path dir;

    @AfterEach
    void clearProps() {
        System.clearProperty(PodSecretsLoaderConfig.PROP_POD_SECRETS_DIR);
    }

    @Test
    void podSecretsSource_isAutoRegistered_whenApplicationBoots() throws Exception {
        Files.writeString(dir.resolve("db_password"), "from-file");
        System.setProperty(PodSecretsLoaderConfig.PROP_POD_SECRETS_DIR, dir.toString());

        SpringApplication application = new SpringApplication(EmptyConfiguration.class);

        try (ConfigurableApplicationContext context = application.run()) {
            assertThat(context.getEnvironment().getPropertySources().contains(PodSecretsPropertySource.SOURCE_NAME))
                    .as("pod-secrets EnvironmentPostProcessor must be auto-registered via spring.factories")
                    .isTrue();
            assertThat(context.getEnvironment().getProperty("DB_PASSWORD")).isEqualTo("from-file");
        }
    }

    @Configuration
    static class EmptyConfiguration {
    }
}
