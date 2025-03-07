package org.qubership.cloud.bluegreen.spring.config;

import org.qubership.cloud.bluegreen.api.service.GlobalMutexService;
import org.qubership.cloud.bluegreen.impl.service.ConsulGlobalMutexService;
import org.qubership.cloud.consul.provider.common.TokenStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.qubership.cloud.bluegreen.spring.config.BlueGreenSpringPropertiesUtil.CONSUL_URL_PROPERTY_SPEL;

/**
 * If AutoConfiguration is turned off, import one of the following Spring Configurations which provide TokenStorage bean
 * 1) org.qubership.cloud.consul.provider.spring.webclient.config.ConsulM2MWebClientAutoConfiguration.class
 * 2) org.qubership.cloud.consul.provider.spring.resttemplate.config.ConsulM2MRestTemplateAutoConfiguration.class
 * see README.md for details
 */
@Configuration
@AutoConfigureAfter(InMemoryConfig.class)
@ConditionalOnProperty(value = "blue-green.global-mutex-service.enabled", havingValue = "true", matchIfMissing = true)
public class BlueGreenGlobalMutexConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public GlobalMutexService globalMutexService(@Value(CONSUL_URL_PROPERTY_SPEL) String consulUrl, TokenStorage tokenStorage) {
        return new ConsulGlobalMutexService(tokenStorage::get, consulUrl);
    }
}
