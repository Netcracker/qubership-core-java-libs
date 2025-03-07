package org.qubership.cloud.bluegreen.spring.config;

import org.qubership.cloud.bluegreen.api.service.BlueGreenStatePublisher;
import org.qubership.cloud.bluegreen.api.service.GlobalMutexService;
import org.qubership.cloud.bluegreen.api.service.MicroserviceMutexService;
import org.qubership.cloud.bluegreen.impl.service.InMemoryBlueGreenStatePublisher;
import org.qubership.cloud.bluegreen.impl.service.InMemoryGlobalMutexService;
import org.qubership.cloud.bluegreen.impl.service.InMemoryMicroserviceMutexService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import static org.qubership.cloud.bluegreen.spring.config.BlueGreenSpringPropertiesUtil.NAMESPACE_PROPERTY_SPEL;

@Configuration
@Conditional(InMemoryCondition.class)
public class InMemoryConfig {

    @Bean
    @ConditionalOnMissingBean
    BlueGreenStatePublisher inMemoryBlueGreenStatePublisher(@Value(NAMESPACE_PROPERTY_SPEL) String namespace) {
        return new InMemoryBlueGreenStatePublisher(namespace);
    }

    @Bean
    @ConditionalOnMissingBean
    GlobalMutexService inMemoryGlobalMutexService() {
        return new InMemoryGlobalMutexService();
    }

    @Bean
    @ConditionalOnMissingBean
    MicroserviceMutexService imMemoryMicroserviceMutexService() {
        return new InMemoryMicroserviceMutexService();
    }
}
