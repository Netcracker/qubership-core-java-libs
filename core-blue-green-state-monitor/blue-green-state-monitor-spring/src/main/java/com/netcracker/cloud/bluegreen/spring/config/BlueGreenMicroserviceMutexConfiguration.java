package com.netcracker.cloud.bluegreen.spring.config;

import com.netcracker.cloud.bluegreen.api.service.MicroserviceMutexService;
import com.netcracker.cloud.bluegreen.impl.service.ConsulMicroserviceMutexService;
import com.netcracker.cloud.bluegreen.impl.util.EnvUtil;
import com.netcracker.cloud.consul.provider.common.TokenStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

import static com.netcracker.cloud.bluegreen.spring.config.BlueGreenSpringPropertiesUtil.*;

/**
 * If AutoConfiguration is turned off, import one of the following Spring Configurations which provide TokenStorage bean
 * 1) com.netcracker.cloud.consul.provider.spring.webclient.config.ConsulM2MWebClientAutoConfiguration.class
 * 2) com.netcracker.cloud.consul.provider.spring.resttemplate.config.ConsulM2MRestTemplateAutoConfiguration.class
 * see README.md for details
 */
@Configuration
@AutoConfigureAfter(InMemoryConfig.class)
@ConditionalOnProperty(value = "blue-green.microservice-mutex-service.enabled", havingValue = "true", matchIfMissing = true)
public class BlueGreenMicroserviceMutexConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MicroserviceMutexService microserviceMutexService(@Value(CONSUL_URL_PROPERTY_SPEL) String consulUrl,
                                                             @Value(NAMESPACE_PROPERTY_SPEL) String namespace,
                                                             @Value(MS_NAME_PROPERTY_SPEL) String name,
                                                             @Value(POD_NAME_PROPERTY_SPEL) String pod,
                                                             TokenStorage tokenStorage) {
        String podName = Optional.ofNullable(pod.isBlank() ? null : pod).orElseGet(EnvUtil::getPodName);
        return new ConsulMicroserviceMutexService(tokenStorage::get, consulUrl, namespace, name, podName);
    }
}
