package org.qubership.maas.declarative.kafka.client.api.model.definition;

import org.qubership.maas.declarative.kafka.client.api.exception.MaasKafkaProducerDefinitionBuildException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class MaasKafkaProducerDefinitionBuildExceptionTest {

    @Test
    void topicNotSpecified() {
        MaasKafkaProducerDefinitionBuildException ex = Assertions.assertThrows(MaasKafkaProducerDefinitionBuildException.class, () -> {
            MaasKafkaProducerDefinition.builder()
                    .setTenant(false)
                    .setClientConfig(Map.of())
                    .build();
        });

        Assertions.assertEquals("Mandatory attribute topic can't be null", ex.getMessage());
    }
}
