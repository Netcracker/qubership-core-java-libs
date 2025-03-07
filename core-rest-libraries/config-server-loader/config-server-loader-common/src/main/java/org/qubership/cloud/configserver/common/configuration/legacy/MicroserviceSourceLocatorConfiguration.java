package org.qubership.cloud.configserver.common.configuration.legacy;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.config.client.ConfigClientProperties;
import org.springframework.cloud.config.client.ConfigServicePropertySourceLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import jakarta.validation.constraints.NotNull;


/**
 * It's configuration for spring boot config and determines config locator bean
 */
@Configuration
@Deprecated // will be deleted in 4.x
public class MicroserviceSourceLocatorConfiguration {
    @Bean
    @Primary
    @Qualifier("microserviceConfigClientProperties")
    public ConfigClientProperties microserviceConfigClientProperties(@NotNull Environment environment) {
        ConfigClientProperties client = new ConfigClientProperties(environment);
        client.setEnabled(false);
        return client;
    }

    @Bean
    @ConditionalOnProperty(prefix = "config.custom_microservice_locator",
            name = "disable",
            havingValue = "false",
            matchIfMissing = true)
    public ConfigServicePropertySourceLocator customConfigServicePropertySource(ConfigClientProperties configClientProperties) {
        return new CustomConfigServerLoader(configClientProperties);
    }
}