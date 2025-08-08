package com.netcracker.maas.declarative.kafka.quarkus.client.config;

import com.netcracker.maas.declarative.kafka.client.api.MaasKafkaClient;
import com.netcracker.maas.declarative.kafka.client.api.MaasKafkaClientStateManagerService;
import com.netcracker.maas.declarative.kafka.client.impl.client.notification.api.MaasKafkaClientStateChangeNotificationService;
import com.netcracker.maas.declarative.kafka.client.impl.client.state.manager.MaasKafkaClientStateManagerImpl;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import java.util.stream.Collectors;

@Singleton
public class MaasKafkaClientStateManagerConfig {

    @Singleton
    @Produces
    MaasKafkaClientStateManagerService maasKafkaClientStateManagerConfig(
            MaasKafkaClientStateChangeNotificationService clientStateChangeNotificationService,
            Instance<MaasKafkaClient> maasKafkaClients
    ) {
        return new MaasKafkaClientStateManagerImpl(
                clientStateChangeNotificationService,
                maasKafkaClients.stream().collect(Collectors.toList()));
    }
}
