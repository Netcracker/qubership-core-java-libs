package org.qubership.maas.declarative.kafka.client.api.model.definition;

import org.qubership.maas.declarative.kafka.client.api.exception.MaasKafkaTopicDefinitionBuildException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MaasTopicDefinitionBuildExceptionTest {

    @Test
    void nameNotSpecified() {
        MaasKafkaTopicDefinitionBuildException exception = Assertions.assertThrows(MaasKafkaTopicDefinitionBuildException.class, () -> {
            MaasTopicDefinition.builder()
                    .setNamespace("maas-test")
                    .setManagedBy(ManagedBy.SELF)
                    .build();
        });

        Assertions.assertEquals("Mandatory attribute 'name' can't be null", exception.getMessage());
    }

    @Test
    void namespaceNotSpecified() {
        MaasKafkaTopicDefinitionBuildException exception = Assertions.assertThrows(MaasKafkaTopicDefinitionBuildException.class, () -> {
            MaasTopicDefinition.builder()
                    .setName("maat-test-topic")
                    .setManagedBy(ManagedBy.SELF)
                    .build();
        });

        Assertions.assertEquals("Mandatory attribute 'namespace' can't be null", exception.getMessage());
    }

    @Test
    void managedByNotSpecified() {
        MaasKafkaTopicDefinitionBuildException exception = Assertions.assertThrows(MaasKafkaTopicDefinitionBuildException.class, () -> {
            MaasTopicDefinition.builder()
                    .setName("maat-test-topic")
                    .setNamespace("maas-test")
                    .build();
        });

        Assertions.assertEquals("Mandatory attribute 'managedBy' can't be null", exception.getMessage());
    }

}
