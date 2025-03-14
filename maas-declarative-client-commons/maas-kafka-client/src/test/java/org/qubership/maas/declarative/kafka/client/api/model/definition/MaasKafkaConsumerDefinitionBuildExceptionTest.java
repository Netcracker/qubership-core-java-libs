package org.qubership.maas.declarative.kafka.client.api.model.definition;

import org.qubership.maas.declarative.kafka.client.api.exception.MaasKafkaConsumerDefinitionBuildException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class MaasKafkaConsumerDefinitionBuildExceptionTest {

    @Test
    void topicDefinitionNotSpecified() {
        MaasKafkaConsumerDefinitionBuildException ex = Assertions.assertThrows(MaasKafkaConsumerDefinitionBuildException.class, () -> {
            MaasKafkaConsumerDefinition.builder()
                    .setGroupId("maas-test")
                    .setInstanceCount(1)
                    .setTenant(false)
                    .setClientConfig(Map.of())
                    .build();
        });

        Assertions.assertEquals("Mandatory attribute topic can't be null", ex.getMessage());
    }

    @Test
    void groupIdNotSpecified() {
        MaasKafkaConsumerDefinitionBuildException ex = Assertions.assertThrows(MaasKafkaConsumerDefinitionBuildException.class, () -> {
            MaasKafkaConsumerDefinition.builder()
                    .setTopic(
                            MaasTopicDefinition.builder()
                                    .setName("maat-test-topic")
                                    .setNamespace("maas-test")
                                    .setManagedBy(ManagedBy.SELF)
                                    .build()
                    )
                    .setInstanceCount(1)
                    .setTenant(false)
                    .setClientConfig(Map.of())
                    .build();
        });

        Assertions.assertEquals("Mandatory attribute groupId can't be null", ex.getMessage());
    }
}
