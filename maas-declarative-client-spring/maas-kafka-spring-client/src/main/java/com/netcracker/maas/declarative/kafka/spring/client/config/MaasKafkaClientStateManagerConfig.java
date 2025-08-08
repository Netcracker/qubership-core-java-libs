package com.netcracker.maas.declarative.kafka.spring.client.config;

import org.qubership.maas.declarative.kafka.client.api.MaasKafkaClient;
import org.qubership.maas.declarative.kafka.client.api.MaasKafkaClientStateManagerService;
import org.qubership.maas.declarative.kafka.client.impl.client.notification.api.MaasKafkaClientStateChangeNotificationService;
import org.qubership.maas.declarative.kafka.client.impl.client.state.manager.MaasKafkaClientStateManagerImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
public class MaasKafkaClientStateManagerConfig {

    @Bean
    MaasKafkaClientStateManagerService maasKafkaClientStateManagerConfigService(
            MaasKafkaClientStateChangeNotificationService clientStateChangeNotificationService,
            List<MaasKafkaClient> maasKafkaClients
    ) {
        return new MaasKafkaClientStateManagerImpl(
                clientStateChangeNotificationService,
                maasKafkaClients
        );
    }
}
