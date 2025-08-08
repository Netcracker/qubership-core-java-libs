package com.netcracker.cloud.dbaas.client.config;

import com.netcracker.cloud.dbaas.client.config.metrics.PostgresMetricsConfiguration;
import com.netcracker.cloud.dbaas.client.metrics.MetricsProvider;
import com.netcracker.cloud.dbaas.client.testconfiguration.TestPostgresConfig;
import io.micrometer.core.instrument.MeterRegistry;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PostgresMetricsConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PostgresMetricsConfiguration.class));

    @Test
    void testWithMicrometerAndPostgresConfig_MetricsPropertyDisabled() {
        contextRunner.withUserConfiguration(MeterRegistryMockConfig.class, TestPostgresConfig.class)
                .withPropertyValues("dbaas.postgres.metrics.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(MetricsProvider.class);
                });
    }

    @Test
    void testWithMicrometerAndPostgresConfig_MetricsPropertyDefault() {
        contextRunner.withUserConfiguration(MeterRegistryMockConfig.class, TestPostgresConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(MetricsProvider.class);
                });
    }

    @Test
    void testOnlyPostgresConfig() {
        contextRunner.withUserConfiguration(TestPostgresConfig.class)
                .run(context -> {
                    assertThat(context).doesNotHaveBean(MetricsProvider.class);
                });
    }

    @Test
    void testOnlyMicrometerConfig() {
        contextRunner.withUserConfiguration(MeterRegistryMockConfig.class)
                .run(context -> {
                    assertThat(context).doesNotHaveBean(MetricsProvider.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class MeterRegistryMockConfig {
        @Bean
        MeterRegistry meterRegistry() {
            return mock(MeterRegistry.class);
        }
    }
}
