package com.netcracker.maas.declarative.kafka.spring.client.config;

import org.qubership.maas.declarative.kafka.client.api.MaasKafkaTopicService;
import org.qubership.maas.declarative.kafka.client.impl.topic.MaasKafkaAggregationTopicService;
import org.qubership.maas.declarative.kafka.client.impl.topic.provider.api.MaasKafkaTopicServiceProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MaasKafkaTopicServiceConfig {

    @Bean
    MaasKafkaTopicService maasKafkaTopicService(
            List<MaasKafkaTopicServiceProvider> topicServiceProviders
    ) {
        return new MaasKafkaAggregationTopicService(topicServiceProviders);
    }

}
