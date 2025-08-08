package com.netcracker.maas.declarative.kafka.client.impl.common.cred.extractor;

import com.netcracker.cloud.maas.client.api.kafka.TopicAddress;
import com.netcracker.maas.declarative.kafka.client.api.model.definition.MaasKafkaCommonClientDefinition;
import com.netcracker.maas.declarative.kafka.client.api.model.definition.MaasKafkaProducerDefinition;
import com.netcracker.maas.declarative.kafka.client.api.model.definition.MaasTopicDefinition;
import com.netcracker.maas.declarative.kafka.client.api.model.definition.ManagedBy;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class PlainTextMaasCredExtractorTest extends AbstractCredExtractorTest {

    @Test
    void testPlainTextTopicExtractor() {
        MaasTopicDefinition topicDefinition = MaasTopicDefinition.builder()
                .setName("/org/qubership/maas/declarative/kafka/client/impl/common/cred/extractor/PLAINTEXT.json")// Path in resource folder
                .setNamespace("test")
                .setManagedBy(ManagedBy.SELF)
                .build();

        MaasKafkaCommonClientDefinition clientDefinition = MaasKafkaProducerDefinition.builder()
                .setTopic(topicDefinition)
                .setTenant(false)
                .build();

        TopicAddress topic = maasKafkaTopicService.getTopicAddressByDefinition(clientDefinition);


        Map<String, Object> extractedCreds = credentialsExtractor.extract(topic);


        assertThat(extractedCreds.get("bootstrap.servers")).isEqualTo("server1:9092,server2:9092");
        assertThat(extractedCreds.get("security.protocol")).isEqualTo("PLAINTEXT");
        assertThat(extractedCreds.get("sasl.mechanism")).isEqualTo("PLAIN");
        assertThat(extractedCreds.get("sasl.jaas.config"))
                .isEqualTo("org.apache.kafka.common.security.plain.PlainLoginModule required username=\"admin\" password=\"admin\";");
    }

}
