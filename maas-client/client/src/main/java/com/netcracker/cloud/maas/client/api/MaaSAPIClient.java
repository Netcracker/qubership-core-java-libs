package com.netcracker.cloud.maas.client.api;

import org.qubership.cloud.maas.client.api.kafka.KafkaMaaSClient;
import org.qubership.cloud.maas.client.api.rabbit.RabbitMaaSClient;

public interface MaaSAPIClient {
    KafkaMaaSClient getKafkaClient();

    RabbitMaaSClient getRabbitClient();
}
