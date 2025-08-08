package com.netcracker.cloud.maas.bluegreen.kafka.impl;

import com.netcracker.cloud.bluegreen.impl.service.InMemoryBlueGreenStatePublisher;
import com.netcracker.cloud.maas.client.impl.Env;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import static com.netcracker.cloud.framework.contexts.xversion.XVersionContextObject.X_VERSION_SERIALIZATION_NAME;
import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
@Slf4j
class DefaultKafkaConsumerTest {
    private final String TOPIC_NAME = "orders";
    private final String NAMESPACE = "test-namespace";
    private Duration POLL_TIMEOUT = Duration.ofSeconds(10);
    private RecordHeaders v2headers = new RecordHeaders(List.of(new RecordHeader(X_VERSION_SERIALIZATION_NAME, "v2".getBytes())));

    Callback callback = (r, e) -> {
        Assertions.assertNull(e);
        log.info("Saved record : {}", r);
    };

    Admin admin;
    String bootstrapServers;
    Properties producerProps;

    @Container
    KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0")).withKraft();

    @BeforeEach
    void setupKafka() {
        bootstrapServers = kafkaContainer.getBootstrapServers();
        Properties props = new Properties();
        props.put(BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        admin = Admin.create(props);
        admin.createTopics(List.of(new NewTopic(TOPIC_NAME, 1, (short) 1)));

        producerProps = new Properties();
        producerProps.putAll(Map.of(ProducerConfig.CLIENT_ID_CONFIG, "test-preparation",
                BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName(),
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName()));
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps)) {
            producer.send(new ProducerRecord<>(TOPIC_NAME, 0, "1", "<order1>"), callback);
            producer.send(new ProducerRecord<>(TOPIC_NAME, 0, "2", "<order2>"), callback);
            producer.send(new ProducerRecord<>(TOPIC_NAME, 0, "3", "<order3>", v2headers), callback);
        }
    }

    @AfterEach
    void cleanup() {
        Optional.ofNullable(admin).ifPresent(Admin::close);
    }

    @Timeout(60)
    @Test
    void testConsumer() {
        System.setProperty(Env.PROP_NAMESPACE, NAMESPACE);

        var properties = Map.<String, Object>of(
                BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                GROUP_ID_CONFIG, "default",
                ENABLE_AUTO_COMMIT_CONFIG, "false",
                AUTO_OFFSET_RESET_CONFIG, "earliest",
                KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class
        );
        var config = BGKafkaConsumerConfig.builder(properties, TOPIC_NAME, () -> "fake", new InMemoryBlueGreenStatePublisher(Env.namespace())).build();

        try (var consumer = new DefaultKafkaConsumer<String, String>(config)) {
            var records = consumer.poll(POLL_TIMEOUT).get();
            assertEquals(3, records.getBatch().size());
            assertEquals(records.getCommitMarker(), records.getBatch().get(2).getCommitMarker());

            String recordData = records.getBatch().get(0).getConsumerRecord().value();
            assertEquals("<order1>", recordData);

            // commit only first record offset
            consumer.commitSync(records.getBatch().get(0).getCommitMarker());
        }

        try (var consumer = new DefaultKafkaConsumer<String, String>(config)) {
            var records = consumer.poll(POLL_TIMEOUT).get();
            assertEquals(2, records.getBatch().size());
        }
    }

    @Timeout(60)
    @Test
    void testPartitionedTopicOffset() throws Exception {
        System.setProperty(Env.PROP_NAMESPACE, NAMESPACE);

        var PRICES_TOPIC = "prices";
        admin.createTopics(List.of(new NewTopic(PRICES_TOPIC, 2, (short) 1)));
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps)) {
            producer.send(new ProducerRecord<>(PRICES_TOPIC, 0, "1", "discount1"), callback);
            producer.send(new ProducerRecord<>(PRICES_TOPIC, 1, "2", "discount2"), callback);
            producer.send(new ProducerRecord<>(PRICES_TOPIC, 1, "3", "discount3"), callback);
        }

        var properties = Map.<String, Object>of(
                BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                GROUP_ID_CONFIG, "c2",
                ENABLE_AUTO_COMMIT_CONFIG, "false",
                AUTO_OFFSET_RESET_CONFIG, "earliest",
                KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class
        );
        var config = BGKafkaConsumerConfig.builder(properties, PRICES_TOPIC, () -> "fake", new InMemoryBlueGreenStatePublisher(Env.namespace())).build();

        try (var consumer = new DefaultKafkaConsumer<String, String>(config)) {
            var records = consumer.poll(POLL_TIMEOUT).get();
            assertEquals(3, records.getBatch().size());
            assertEquals(
                    Map.of(
                            new TopicPartition(PRICES_TOPIC, 0), new OffsetAndMetadata(1),
                            new TopicPartition(PRICES_TOPIC, 1), new OffsetAndMetadata(2)
                    ),
                    records.getCommitMarker().getPosition());
        }
    }
}
