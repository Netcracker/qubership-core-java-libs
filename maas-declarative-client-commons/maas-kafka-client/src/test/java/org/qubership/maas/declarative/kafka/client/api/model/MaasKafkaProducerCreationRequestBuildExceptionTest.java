package org.qubership.maas.declarative.kafka.client.api.model;

import org.qubership.maas.declarative.kafka.client.api.exception.MaasKafkaProducerCreationRequestBuildException;
import org.qubership.maas.declarative.kafka.client.api.model.definition.MaasKafkaProducerDefinition;
import org.qubership.maas.declarative.kafka.client.api.model.definition.MaasTopicDefinition;
import org.qubership.maas.declarative.kafka.client.api.model.definition.ManagedBy;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class MaasKafkaProducerCreationRequestBuildExceptionTest {

    @Test
    void definitionNotSpecified() {
        MaasKafkaProducerCreationRequestBuildException ex = Assertions.assertThrows(MaasKafkaProducerCreationRequestBuildException.class, () -> {
            MaasKafkaProducerCreationRequest.builder()
                    .setKeySerializer(new StringSerializer())
                    .setValueSerializer(new StringSerializer())
                    .setHandler(producerRecord -> producerRecord)
                    .build();
        });

        Assertions.assertEquals("MaasKafkaProducerDefinition can't be null", ex.getMessage());
    }

    @Test
    void keySerializerNotSpecified() {
        MaasKafkaProducerCreationRequestBuildException ex = Assertions.assertThrows(MaasKafkaProducerCreationRequestBuildException.class, () -> {
            MaasTopicDefinition topicDefinition = MaasTopicDefinition.builder()
                    .setName("maas-test-topic")
                    .setNamespace("maas-local")
                    .setManagedBy(ManagedBy.SELF)
                    .build();

            MaasKafkaProducerDefinition maasKafkaProducerDefinition = MaasKafkaProducerDefinition.builder()
                    .setTenant(false)
                    .setTopic(topicDefinition)
                    .setClientConfig(Map.of())
                    .build();

            MaasKafkaProducerCreationRequest.builder()
                    .setProducerDefinition(maasKafkaProducerDefinition)
                    .setValueSerializer(new StringSerializer())
                    .setHandler(producerRecord -> producerRecord)
                    .build();
        });

        Assertions.assertTrue(ex.getMessage().startsWith("There is no key serializer for producer org.qubership.maas.declarative.kafka.client.api.model.definition.MaasKafkaProducerDefinition"));
    }

    @Test
    void valueSerializerNotSpecified() {
        MaasKafkaProducerCreationRequestBuildException ex = Assertions.assertThrows(MaasKafkaProducerCreationRequestBuildException.class, () -> {
            MaasTopicDefinition topicDefinition = MaasTopicDefinition.builder()
                    .setName("maas-test-topic")
                    .setNamespace("maas-local")
                    .setManagedBy(ManagedBy.SELF)
                    .build();

            MaasKafkaProducerDefinition maasKafkaProducerDefinition = MaasKafkaProducerDefinition.builder()
                    .setTenant(false)
                    .setTopic(topicDefinition)
                    .setClientConfig(Map.of())
                    .build();

            MaasKafkaProducerCreationRequest.builder()
                    .setProducerDefinition(maasKafkaProducerDefinition)
                    .setKeySerializer(new StringSerializer())
                    .setHandler(producerRecord -> producerRecord)
                    .build();
        });

        System.out.println(ex.getMessage());
        Assertions.assertTrue(ex.getMessage().startsWith("There is no value serializer for producer org.qubership.maas.declarative.kafka.client.api.model.definition.MaasKafkaProducerDefinition"));
    }
}
