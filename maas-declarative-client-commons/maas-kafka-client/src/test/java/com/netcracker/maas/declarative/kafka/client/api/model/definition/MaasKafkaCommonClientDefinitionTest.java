package com.netcracker.maas.declarative.kafka.client.api.model.definition;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MaasKafkaCommonClientDefinitionTest {

    @Test
    void getClientConfig() {
        MaasKafkaConsumerDefinition maasKafkaConsumerDefinition = MaasKafkaConsumerDefinition.builder()
                .setTopic(MaasTopicDefinition.builder()
                        .setName("test")
                        .setNamespace("ns")
                        .setManagedBy(ManagedBy.MAAS)
                        .build())
                .setGroupId("gr")
                .build();
        assertNotNull(maasKafkaConsumerDefinition);
        assertNotNull(maasKafkaConsumerDefinition.getClientConfig());
    }
}
