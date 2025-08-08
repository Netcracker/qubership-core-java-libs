package com.netcracker.maas.declarative.kafka.client.impl.topic.provider.impl;

import org.qubership.cloud.maas.client.api.kafka.KafkaMaaSClient;
import org.qubership.maas.declarative.kafka.client.api.MaasKafkaTopicService;
import org.qubership.maas.declarative.kafka.client.impl.topic.MaasKafkaTopicServiceImpl;
import org.qubership.maas.declarative.kafka.client.impl.topic.provider.api.MaasKafkaTopicServiceProvider;

public class MaasKafkaTopicServiceProviderImpl implements MaasKafkaTopicServiceProvider {

    private final MaasKafkaTopicService maasKafkaTopicService;

    public MaasKafkaTopicServiceProviderImpl(KafkaMaaSClient maasKafkaClient) {
        maasKafkaTopicService = new MaasKafkaTopicServiceImpl(maasKafkaClient);
    }

    @Override
    public MaasKafkaTopicService provide() {
        return maasKafkaTopicService;
    }
}
