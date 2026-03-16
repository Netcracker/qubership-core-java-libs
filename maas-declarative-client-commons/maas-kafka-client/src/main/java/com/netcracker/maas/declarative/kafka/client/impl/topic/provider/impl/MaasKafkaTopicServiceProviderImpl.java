package com.netcracker.maas.declarative.kafka.client.impl.topic.provider.impl;

import com.netcracker.cloud.maas.client.api.kafka.KafkaMaaSClient;
import com.netcracker.maas.declarative.kafka.client.api.MaasKafkaTopicService;
import com.netcracker.maas.declarative.kafka.client.impl.topic.MaasKafkaTopicServiceImpl;
import com.netcracker.maas.declarative.kafka.client.impl.topic.provider.api.MaasKafkaTopicServiceProvider;

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
