package com.netcracker.maas.declarative.kafka.client.impl.common.cred.extractor;

import org.qubership.cloud.maas.client.api.kafka.TopicAddress;
import org.qubership.maas.declarative.kafka.client.api.model.definition.MaasKafkaCommonClientDefinition;
import org.qubership.maas.declarative.kafka.client.api.model.definition.MaasKafkaProducerDefinition;
import org.qubership.maas.declarative.kafka.client.api.model.definition.MaasTopicDefinition;
import org.qubership.maas.declarative.kafka.client.api.model.definition.ManagedBy;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ScramMaasCredExtractorTest extends AbstractCredExtractorTest {

    @Test
    void testScramMaasCredExtractor() {
        MaasTopicDefinition topicDefinition = MaasTopicDefinition.builder()
                .setName("/org/qubership/maas/declarative/kafka/client/impl/common/cred/extractor/SASL_PLAINTEXT.json")// Path in resource folder
                .setNamespace("test")
                .setManagedBy(ManagedBy.SELF)
                .build();

        MaasKafkaCommonClientDefinition clientDefinition = MaasKafkaProducerDefinition.builder()
                .setTopic(topicDefinition)
                .setTenant(false)
                .build();

        TopicAddress topic = maasKafkaTopicService.getTopicAddressByDefinition(clientDefinition);

        Map<String, Object> extractedCreds = credentialsExtractor.extract(topic);


        assertThat(extractedCreds.get("bootstrap.servers")).isEqualTo("kafka.maas-kafka:9092");
        assertThat(extractedCreds.get("security.protocol")).isEqualTo("SASL_PLAINTEXT");
        assertThat(extractedCreds.get("sasl.mechanism")).isEqualTo("SCRAM-SHA-512");
        assertThat(extractedCreds.get("sasl.jaas.config"))
                .isEqualTo("org.apache.kafka.common.security.scram.ScramLoginModule required username=\"client\" password=\"client\";");
    }
}
