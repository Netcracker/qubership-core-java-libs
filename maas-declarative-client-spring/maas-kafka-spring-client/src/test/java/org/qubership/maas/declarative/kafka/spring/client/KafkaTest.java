package org.qubership.maas.declarative.kafka.spring.client;

import org.qubership.maas.declarative.kafka.client.api.MaasKafkaClientFactory;
import org.qubership.maas.declarative.kafka.client.api.MaasKafkaConsumer;
import org.qubership.maas.declarative.kafka.client.api.MaasKafkaProducer;
import org.qubership.maas.declarative.kafka.client.api.model.MaasKafkaConsumerCreationRequest;
import org.qubership.maas.declarative.kafka.client.api.model.MaasKafkaProducerCreationRequest;
import org.qubership.maas.declarative.kafka.client.api.model.definition.MaasKafkaConsumerDefinition;
import org.qubership.maas.declarative.kafka.client.api.model.definition.MaasKafkaProducerDefinition;
import org.qubership.maas.declarative.kafka.client.api.model.definition.MaasTopicDefinition;
import net.mguenther.kafka.junit.EmbeddedKafkaCluster;
import net.mguenther.kafka.junit.EmbeddedKafkaClusterConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.support.TestPropertySourceUtils;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import static net.mguenther.kafka.junit.EmbeddedKafkaCluster.provisionWith;
import static net.mguenther.kafka.junit.EmbeddedKafkaClusterConfig.defaultClusterConfig;

public class KafkaTest implements ApplicationContextAware {
    protected static EmbeddedKafkaCluster kafka;

    @BeforeAll
    static void beforeAll() {
        EmbeddedKafkaClusterConfig kafkaClusterConfig = defaultClusterConfig();
        kafka = provisionWith(kafkaClusterConfig);
        kafka.start();

    }

    @AfterAll
    static void afterAll() {
        kafka.stop();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                (ConfigurableApplicationContext) applicationContext, "maas.kafka.local-dev.config.bootstrap.servers=localhost:9092");
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
