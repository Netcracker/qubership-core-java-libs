package com.netcracker.cloud.dbaas.client.restclient.resttemplate;

import com.netcracker.cloud.security.core.auth.DummyM2MManager;
import com.netcracker.cloud.security.core.auth.M2MManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DbaasRestTemplateConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DbaasRestTemplateConfiguration.class));

    @Test
    void whenNoM2MManagerBean_thenDummyIsRegistered() {
        contextRunner.run(context ->
                assertThat(context).hasSingleBean(M2MManager.class)
                        .getBean(M2MManager.class).isInstanceOf(DummyM2MManager.class)
        );
    }

    @Test
    void whenRealM2MManagerPresent_thenDummyIsNotRegistered() {
        contextRunner.withUserConfiguration(RealM2MManagerConfig.class).run(context -> {
            assertThat(context).hasSingleBean(M2MManager.class);
            assertThat(context.getBean(M2MManager.class)).isNotInstanceOf(DummyM2MManager.class);
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class RealM2MManagerConfig {
        @Bean
        M2MManager m2mManager() {
            return mock(M2MManager.class);
        }
    }
}
