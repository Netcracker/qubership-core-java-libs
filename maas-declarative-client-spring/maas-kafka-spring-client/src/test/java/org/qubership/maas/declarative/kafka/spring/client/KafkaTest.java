package org.qubership.maas.declarative.kafka.spring.client;

import org.qubership.maas.declarative.kafka.client.api.MaasKafkaClientFactory;
import org.qubership.maas.declarative.kafka.client.api.MaasKafkaConsumer;
import org.qubership.maas.declarative.kafka.client.api.MaasKafkaProducer;
import org.qubership.maas.declarative.kafka.client.api.model.MaasKafkaConsumerCreationRequest;
import org.qubership.maas.declarative.kafka.client.api.model.MaasKafkaProducerCreationRequest;
import org.qubership.maas.declarative.kafka.client.api.model.definition.MaasKafkaConsumerDefinition;
import org.qubership.maas.declarative.kafka.client.api.model.definition.MaasKafkaProducerDefinition;
import org.qubership.maas.declarative.kafka.client.api.model.definition.MaasTopicDefinition;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

@Testcontainers
public class KafkaTest {

    public static String bootstrapServers;

    @Container
    static KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"))
            .withKraft();

    @BeforeAll
    static void setup() {
        bootstrapServers = kafkaContainer.getBootstrapServers().replace("PLAINTEXT://", "");
    }

    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("maas.kafka.local-dev.config.bootstrap.servers", () -> bootstrapServers);
    }

    protected <K, V> MaasKafkaConsumer createAndActivateConsumer(MaasKafkaClientFactory clientFactory, String definition, String topic, Consumer<ConsumerRecord<K, V>> handler) {
        MaasKafkaConsumer consumer = createConsumer(clientFactory, definition, topic, handler);
        consumer.initSync();
        consumer.activateSync();
        return consumer;
    }

    protected <K, V> MaasKafkaConsumer createConsumer(MaasKafkaClientFactory clientFactory, String definition, String topic, Consumer<ConsumerRecord<K, V>> handler) {
        MaasKafkaConsumerDefinition consumerDefinition = clientFactory.getConsumerDefinition(definition);
        MaasKafkaConsumerDefinition.Builder consumerDefinitionbuilder = MaasKafkaConsumerDefinition.builder(consumerDefinition);
        if (Strings.isNotEmpty(topic)) {
            consumerDefinitionbuilder
                    .setGroupId("group-" + topic)
                    .setTopic(MaasTopicDefinition.builder(consumerDefinition.getTopic())
                            .setName(topic).build())
                    .build();
        }
        MaasKafkaConsumerCreationRequest consumerCreationRequest =
                MaasKafkaConsumerCreationRequest.builder()
                        .setConsumerDefinition(consumerDefinitionbuilder.build())
                        .setHandler(handler)
                        .setValueDeserializer(new StrTestDeserializer())
                        .build();

        return clientFactory.createConsumer(consumerCreationRequest);
    }

    protected <K, V> MaasKafkaConsumer createConsumer(MaasKafkaClientFactory clientFactory, String definition, Consumer<ConsumerRecord<K, V>> handler) {
        return createConsumer(clientFactory, definition, null, handler);
    }

    protected MaasKafkaProducer createAndActivateProducer(MaasKafkaClientFactory clientFactory, String definition, String topic) {
        MaasKafkaProducer producer = createProducer(clientFactory, definition, topic);
        producer.initSync();
        producer.activateSync();
        return producer;
    }

    protected MaasKafkaProducer createProducer(MaasKafkaClientFactory clientFactory, String definition, String topic) {
        MaasKafkaProducerDefinition producerDefinition = clientFactory.getProducerDefinition(definition);
        MaasKafkaProducerDefinition.Builder producerDefinitionbuilder = MaasKafkaProducerDefinition.builder(producerDefinition);
        if (Strings.isNotEmpty(topic)) {
            producerDefinitionbuilder.setTopic(MaasTopicDefinition.builder(producerDefinition.getTopic()).setName(topic).build()).build();
        }
        MaasKafkaProducerCreationRequest producerCreationRequest =
                MaasKafkaProducerCreationRequest.builder()
                        .setProducerDefinition(producerDefinitionbuilder.build())
                        .setHandler(r -> r)
                        .setValueSerializer(new StrTestSerializer())
                        .build();

        return clientFactory.createProducer(producerCreationRequest);
    }

    protected MaasKafkaProducer createProducer(MaasKafkaClientFactory clientFactory, String definition) {
        return createProducer(clientFactory, definition, null);
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
