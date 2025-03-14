package org.qubership.maas.declarative.kafka.client.api.model;

import org.qubership.maas.declarative.kafka.client.api.exception.MaasKafkaConsumerCreationRequestBuildException;
import org.qubership.maas.declarative.kafka.client.api.model.definition.MaasKafkaConsumerDefinition;
import org.qubership.maas.declarative.kafka.client.api.model.definition.MaasTopicDefinition;
import org.qubership.maas.declarative.kafka.client.api.model.definition.ManagedBy;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;


public class MaasKafkaConsumerCreationRequestBuildExceptionTest {

    @Test
    void consumerDefinitionNotSpecified() {
        MaasKafkaConsumerCreationRequestBuildException ex = Assertions.assertThrows(MaasKafkaConsumerCreationRequestBuildException.class, () -> {
            MaasKafkaConsumerCreationRequest.builder()
                    .setKeyDeserializer(new StringDeserializer())
                    .setValueDeserializer(new StringDeserializer())
                    .setHandler(record -> {
                    })
                    .build();
        });

        Assertions.assertEquals("MaasKafkaConsumerDefinition can't be null", ex.getMessage());
    }

    @Test
    void handlerNotSpecified() {
        MaasKafkaConsumerCreationRequestBuildException ex = Assertions.assertThrows(MaasKafkaConsumerCreationRequestBuildException.class, () -> {
            MaasTopicDefinition topicDefinition = MaasTopicDefinition.builder()
                    .setName("maas-test-topic")
                    .setNamespace("maas-local")
                    .setManagedBy(ManagedBy.SELF)
                    .build();

            MaasKafkaConsumerDefinition consumerDefinition = MaasKafkaConsumerDefinition.builder()
                    .setGroupId("maas-test")
                    .setInstanceCount(1)
                    .setTenant(false)
                    .setTopic(topicDefinition)
                    .setClientConfig(Map.of())
                    .build();

            MaasKafkaConsumerCreationRequest.builder()
                    .setConsumerDefinition(consumerDefinition)
                    .setKeyDeserializer(new StringDeserializer())
                    .setValueDeserializer(new StringDeserializer())
                    .build();
        });

        Assertions.assertEquals("Record handler can't be null", ex.getMessage());
    }

    @Test
    void keyDeserializerNotSpecified() {
        MaasKafkaConsumerCreationRequestBuildException ex = Assertions.assertThrows(MaasKafkaConsumerCreationRequestBuildException.class, () -> {
            MaasTopicDefinition topicDefinition = MaasTopicDefinition.builder()
                    .setName("maas-test-topic")
                    .setNamespace("maas-local")
                    .setManagedBy(ManagedBy.SELF)
                    .build();

            MaasKafkaConsumerDefinition consumerDefinition = MaasKafkaConsumerDefinition.builder()
                    .setGroupId("maas-test")
                    .setInstanceCount(1)
                    .setTenant(false)
                    .setTopic(topicDefinition)
                    .setClientConfig(Map.of())
                    .build();

            MaasKafkaConsumerCreationRequest.builder()
                    .setConsumerDefinition(consumerDefinition)
                    .setValueDeserializer(new StringDeserializer())
                    .setHandler(record -> {
                    })
                    .build();
        });

        Assertions.assertTrue(ex.getMessage().startsWith("There is no key deserializer for consumer org.qubership.maas.declarative.kafka.client.api.model.definition.MaasKafkaConsumerDefinition"));
    }

    @Test
    void valueDeserializerNotSpecified() {
        MaasKafkaConsumerCreationRequestBuildException ex = Assertions.assertThrows(MaasKafkaConsumerCreationRequestBuildException.class, () -> {
            MaasTopicDefinition topicDefinition = MaasTopicDefinition.builder()
                    .setName("maas-test-topic")
                    .setNamespace("maas-local")
                    .setManagedBy(ManagedBy.SELF)
                    .build();

            MaasKafkaConsumerDefinition consumerDefinition = MaasKafkaConsumerDefinition.builder()
                    .setGroupId("maas-test")
                    .setInstanceCount(1)
                    .setTenant(false)
                    .setTopic(topicDefinition)
                    .setClientConfig(Map.of())
                    .build();

            MaasKafkaConsumerCreationRequest.builder()
                    .setConsumerDefinition(consumerDefinition)
                    .setKeyDeserializer(new StringDeserializer())
                    .setHandler(record -> {
                    })
                    .build();
        });

        Assertions.assertTrue(ex.getMessage().startsWith("There is no value deserializer for consumer org.qubership.maas.declarative.kafka.client.api.model.definition.MaasKafkaConsumerDefinition"));
    }
}
