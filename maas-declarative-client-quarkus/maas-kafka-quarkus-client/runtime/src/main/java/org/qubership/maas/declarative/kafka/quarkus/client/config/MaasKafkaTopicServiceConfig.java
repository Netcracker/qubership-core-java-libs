package org.qubership.maas.declarative.kafka.quarkus.client.config;

import org.qubership.maas.declarative.kafka.client.api.MaasKafkaTopicService;
import org.qubership.maas.declarative.kafka.client.impl.topic.MaasKafkaAggregationTopicService;
import org.qubership.maas.declarative.kafka.client.impl.topic.provider.api.MaasKafkaTopicServiceProvider;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import java.util.stream.Collectors;

@Singleton
public class MaasKafkaTopicServiceConfig {

    @Singleton
    @Produces
    MaasKafkaTopicService maasKafkaTopicService(
            Instance<MaasKafkaTopicServiceProvider> topicServiceProviders
    ) {
        return new MaasKafkaAggregationTopicService(topicServiceProviders.stream().collect(Collectors.toList()));
    }

}
