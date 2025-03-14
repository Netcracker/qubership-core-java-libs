package org.qubership.maas.declarative.kafka.quarkus.client.test;

import org.qubership.maas.declarative.kafka.client.api.MaasKafkaClientFactory;
import org.qubership.maas.declarative.kafka.client.api.MaasKafkaConsumer;
import org.qubership.maas.declarative.kafka.client.api.MaasKafkaProducer;
import org.qubership.maas.declarative.kafka.client.api.model.MaasKafkaConsumerCreationRequest;
import org.qubership.maas.declarative.kafka.client.api.model.MaasKafkaProducerCreationRequest;
import org.qubership.maas.declarative.kafka.client.api.model.MaasProducerRecord;
import org.qubership.maas.declarative.kafka.client.api.model.definition.MaasKafkaConsumerDefinition;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.inject.Inject;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.waitAtMost;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class MonitoringTests extends AbstractMaasKafkaTest {

    private static final int MSG_WAIT_TIME_SEC = 10;

    @Inject
    MaasKafkaClientFactory clientFactory;

    @Inject
    MeterRegistry registry;

    @Test
    void producerIsInstrumented() {
        createProducer().sendSync(new MaasProducerRecord<>(
                null,
                UUID.randomUUID().toString(),
                "test-msg",
                null,
                null
        ));

        assertNotNull(registry.getMeters());
        assertFalse(registry.getMeters().isEmpty());
        waitAtMost(Duration.ofSeconds(MSG_WAIT_TIME_SEC))
                .untilAsserted(() -> assertThat(this.registry.get("kafka.producer.record.send.total").functionCounter().count())
                        .isGreaterThanOrEqualTo(1));
    }

    @Test
    void consumerIsInstrumented() {
        createConsumer();
        createProducer().sendSync(new MaasProducerRecord<>(
                null,
                UUID.randomUUID().toString(),
                "test-msg",
                null,
                null
        ));

        assertNotNull(registry.getMeters());
        assertFalse(registry.getMeters().isEmpty());
        waitAtMost(Duration.ofSeconds(MSG_WAIT_TIME_SEC))
                .untilAsserted(() -> assertThat(this.registry.get("kafka.consumer.fetch.manager.records.consumed.total").functionCounter().count())
                        .isGreaterThanOrEqualTo(1));
    }

    private MaasKafkaConsumer createConsumer() {
        MaasKafkaConsumerDefinition consumerDefinition = clientFactory.getConsumerDefinition("testConsumer");
        MaasKafkaConsumerCreationRequest consumerCreationRequest =
                MaasKafkaConsumerCreationRequest.builder()
                        .setConsumerDefinition(MaasKafkaConsumerDefinition.builder(consumerDefinition).setGroupId("group-" + UUID.randomUUID()).build())
                        .setHandler(consumerRecord -> {
                        })
                        .setValueDeserializer(new StrTestDeserializer())
                        .build();

        MaasKafkaConsumer consumer = clientFactory.createConsumer(consumerCreationRequest);
        consumer.initSync();
        consumer.activateSync();
        return consumer;
    }

    private MaasKafkaProducer createProducer() {
        MaasKafkaProducerCreationRequest producerCreationRequest =
                MaasKafkaProducerCreationRequest.builder()
                        .setProducerDefinition(clientFactory.getProducerDefinition("test-producer"))
                        .setHandler(r -> r)
                        .setValueSerializer(new StrTestSerializer())
                        .build();

        MaasKafkaProducer producer = clientFactory.createProducer(producerCreationRequest);
        producer.initSync();
        producer.activateSync();
        return producer;
    }

    private static class StrTestDeserializer implements Deserializer<String> {
        @Override
        public String deserialize(String topic, byte[] data) {
            return new String(data, StandardCharsets.UTF_8);
        }
    }

    private static class StrTestSerializer implements Serializer<String> {

        @Override
        public byte[] serialize(String topic, String data) {
            return data.getBytes(StandardCharsets.UTF_8);
        }
    }
}