package org.qubership.maas.declarative.kafka.client.impl.topic.provider.api;

import org.qubership.maas.declarative.kafka.client.api.MaasKafkaTopicService;

public interface MaasKafkaTopicServiceProvider {

    MaasKafkaTopicService provide();

    default int order() {
        return Integer.MAX_VALUE;
    }
}
