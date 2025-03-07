package org.qubership.cloud.bluegreen.spring.config;

import org.qubership.cloud.bluegreen.api.service.BlueGreenStatePublisher;
import org.qubership.cloud.bluegreen.impl.service.ConsulBlueGreenStatePublisher;
import org.qubership.cloud.consul.provider.common.TokenStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.qubership.cloud.bluegreen.spring.config.BlueGreenSpringPropertiesUtil.CONSUL_URL_PROPERTY_SPEL;
import static org.qubership.cloud.bluegreen.spring.config.BlueGreenSpringPropertiesUtil.NAMESPACE_PROPERTY_SPEL;

/**
 * If AutoConfiguration is turned off, import one of the following Spring Configurations which provide TokenStorage bean
 * 1) org.qubership.cloud.consul.provider.spring.webclient.config.ConsulM2MWebClientAutoConfiguration.class
 * 2) org.qubership.cloud.consul.provider.spring.resttemplate.config.ConsulM2MRestTemplateAutoConfiguration.class
 * see README.md for details
 */
@Configuration
@AutoConfigureAfter(InMemoryConfig.class)
@ConditionalOnProperty(value = "blue-green.state-publisher.enabled", havingValue = "true", matchIfMissing = true)
public class BlueGreenStatePublisherConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public BlueGreenStatePublisher blueGreenStatePublisher(@Value(CONSUL_URL_PROPERTY_SPEL) String consulUrl,
                                                           @Value(NAMESPACE_PROPERTY_SPEL) String namespace,
                                                           TokenStorage tokenStorage) {
        return new ConsulBlueGreenStatePublisher(tokenStorage::get, consulUrl, namespace);
    }
}
