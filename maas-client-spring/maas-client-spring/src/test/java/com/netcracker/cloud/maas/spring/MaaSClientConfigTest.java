package com.netcracker.cloud.maas.spring;

import com.netcracker.cloud.maas.client.api.MaaSAPIClient;
import com.netcracker.cloud.maas.client.impl.MaaSAPIClientImpl;
import com.netcracker.cloud.security.core.auth.M2MManager;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;

class MaaSClientConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(context -> {
                org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor.applyTo(context.getEnvironment());
            })
            .withSystemProperties("spring.profiles.active=test")
            .withConfiguration(AutoConfigurations.of(MaaSClientConfig.class, MockM2MManagerConfig.class));

    @Test
    void shouldPassTrueToClientConstructor() {
        try (MockedConstruction<MaaSAPIClientImpl> mocked = mockConstruction(MaaSAPIClientImpl.class,
                (mock, context) -> {
                    assertThat(context.arguments().get(1)).isEqualTo(true);
                })) {
            contextRunner.run(context -> {
                assertThat(context).hasSingleBean(MaaSAPIClient.class);
                assertThat(mocked.constructed()).hasSize(1);
            });
        }
    }

    @Configuration
    static class MockM2MManagerConfig {
        @Bean
        M2MManager m2MManager() {
            return mock(M2MManager.class);
        }
    }
}
