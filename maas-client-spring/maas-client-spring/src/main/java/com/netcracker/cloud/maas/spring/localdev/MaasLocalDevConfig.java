package org.qubership.cloud.maas.spring.localdev;

import org.qubership.cloud.bluegreen.api.service.BlueGreenStatePublisher;
import org.qubership.cloud.bluegreen.impl.service.InMemoryBlueGreenStatePublisher;
import org.qubership.cloud.security.core.auth.DummyM2MManager;
import org.qubership.cloud.security.core.auth.M2MManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@ConditionalOnProperty(value = "maas.local-dev.enabled", havingValue = "true")
public class MaasLocalDevConfig {

    @Value("${cloud.microservice.namespace:unknown}")
    String namespace;

    @Bean
    public BlueGreenStatePublisher inMemoryBlueGreenStatePublisher() {
        return new InMemoryBlueGreenStatePublisher(namespace);
    }

    @Bean
    @Primary
    public M2MManager localDevM2MManager() {
        return new DummyM2MManager();
    }
}
