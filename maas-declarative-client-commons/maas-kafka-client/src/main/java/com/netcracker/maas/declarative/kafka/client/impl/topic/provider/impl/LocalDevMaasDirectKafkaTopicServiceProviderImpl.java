package com.netcracker.maas.declarative.kafka.client.impl.topic.provider.impl;

import org.qubership.maas.declarative.kafka.client.api.MaasKafkaTopicService;
import org.qubership.maas.declarative.kafka.client.impl.topic.LocalDevMaasDirectKafkaTopicServiceImpl;
import org.qubership.maas.declarative.kafka.client.impl.local.dev.config.api.MaasKafkaLocalDevConfigProviderService;
import org.qubership.maas.declarative.kafka.client.impl.topic.provider.api.MaasKafkaTopicServiceProvider;

// for development
public class LocalDevMaasDirectKafkaTopicServiceProviderImpl implements MaasKafkaTopicServiceProvider {

    private final MaasKafkaTopicService maasKafkaTopicService;

    public LocalDevMaasDirectKafkaTopicServiceProviderImpl(MaasKafkaLocalDevConfigProviderService configProviderService) {
        maasKafkaTopicService = new LocalDevMaasDirectKafkaTopicServiceImpl(configProviderService);
    }

    @Override
    public MaasKafkaTopicService provide() {
        return maasKafkaTopicService;
    }

    @Override
    public int order() {
        return MaasKafkaTopicServiceProvider.super.order() - 1;// to override default maas topic service
    }

}
