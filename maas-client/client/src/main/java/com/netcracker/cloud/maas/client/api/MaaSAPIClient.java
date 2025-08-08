package com.netcracker.cloud.maas.client.api;

import com.netcracker.cloud.maas.client.api.kafka.KafkaMaaSClient;
import com.netcracker.cloud.maas.client.api.rabbit.RabbitMaaSClient;

public interface MaaSAPIClient {
    KafkaMaaSClient getKafkaClient();

    RabbitMaaSClient getRabbitClient();
}
