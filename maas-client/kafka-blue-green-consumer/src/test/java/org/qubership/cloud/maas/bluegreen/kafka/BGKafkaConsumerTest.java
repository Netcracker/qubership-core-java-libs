package org.qubership.cloud.maas.bluegreen.kafka;

import org.qubership.cloud.bluegreen.api.model.BlueGreenState;
import org.qubership.cloud.bluegreen.api.model.NamespaceVersion;
import org.qubership.cloud.bluegreen.api.model.State;
import org.qubership.cloud.bluegreen.api.model.Version;
import org.qubership.cloud.bluegreen.impl.service.InMemoryBlueGreenStatePublisher;
import org.qubership.cloud.maas.bluegreen.kafka.impl.AdminAdapterImpl;
import org.qubership.cloud.maas.bluegreen.kafka.impl.BGKafkaConsumerConfig;
import org.qubership.cloud.maas.bluegreen.kafka.impl.BGKafkaConsumerImpl;
import org.qubership.cloud.maas.bluegreen.kafka.impl.OffsetsIndexer;
import org.qubership.cloud.maas.client.impl.Env;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.KafkaConsumer;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.qubership.cloud.framework.contexts.xversion.XVersionContextObject.X_VERSION_SERIALIZATION_NAME;
import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@Testcontainers
class BGKafkaConsumerTest {
    static String NAMESPACE_1 = "test-ns-1";
    static String NAMESPACE_2 = "test-ns-2";
    static String TOPIC_NAME = "orders";
    static Supplier<String> M2M_TOKEN_SUPPLIER = () -> "fake";
    static Supplier<String> CONSUL_TOKEN_SUPPLIER = () -> "fake";

    private RecordHeaders v1headers = new RecordHeaders(List.of(new RecordHeader(X_VERSION_SERIALIZATION_NAME, "v1".getBytes())));
    private RecordHeaders v2headers = new RecordHeaders(List.of(new RecordHeader(X_VERSION_SERIALIZATION_NAME, "v2".getBytes())));
    private Duration POLL_TIMEOUT = Duration.ofSeconds(10);

    @Container
    KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0")).withKraft();

    Admin admin;
    String bootstrapServers;
    Properties producerProps;

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
        Callback callback = (r, e) -> {
            Assertions.assertNull(e);
            log.info("Saved record : {}", r);
        };
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps)) {
            producer.send(new ProducerRecord<>(TOPIC_NAME, 0, "1", "<order1>"), callback);
            producer.send(new ProducerRecord<>(TOPIC_NAME, 0, "2", "<order2>"), callback);
            producer.send(new ProducerRecord<>(TOPIC_NAME, 0, "3", "<order3>"), callback);
        }
    }

    @AfterEach
    void cleanup() {
        Optional.ofNullable(admin).ifPresent(Admin::close);
    }

    // no offset exists in kafka, test to set brand-new group id according to initial offset set policies
    @Test
    void testNewOffsetSet() throws Exception {
        System.setProperty(Env.PROP_NAMESPACE, NAMESPACE_1);
        var connectionProperties = Map.<String, Object>of(
                "bootstrap.servers", bootstrapServers,
                "group.id", "brandnew",
                "enable.auto.commit", "false");

        Supplier<BGKafkaConsumerImpl<String, String>> consumerSupplier = () -> new BGKafkaConsumerImpl<>(BGKafkaConsumerConfig.builder(connectionProperties,
                        TOPIC_NAME, M2M_TOKEN_SUPPLIER, new InMemoryBlueGreenStatePublisher(Env.namespace()))
                .deserializers(new StringDeserializer(), new StringDeserializer())
                .consistencyMode(ConsumerConsistencyMode.GUARANTEE_CONSUMPTION).build());

        try (BGKafkaConsumerImpl<String, String> consumer = consumerSupplier.get()) {
            // explicitly define types to prevent generics misconfigurations
            RecordsBatch<String, String> records = consumer.poll(POLL_TIMEOUT).get();
            assertEquals(3, records.getBatch().size());
            assertEquals(records.getCommitMarker(), records.getBatch().get(2).getCommitMarker());

            String recordData = records.getBatch().get(0).getConsumerRecord().value();
            assertEquals("<order1>", recordData);

            // commit only first record offset
            consumer.commitSync(records.getBatch().get(0).getCommitMarker());
        }

        // check that we on right position after previous run, right after record #1
        try (BGKafkaConsumerImpl<String, String> consumer = consumerSupplier.get()) {
            var records = consumer.poll(POLL_TIMEOUT).get();
            assertEquals(2, records.getBatch().size());
        }
    }

    // test migration from non-bg to bg-aware consumer
    @Test
    void testOffsetMigration() throws Exception {
        System.setProperty(Env.PROP_NAMESPACE, NAMESPACE_1);
        var connectionProperties = Map.<String, Object>of(
                "bootstrap.servers", bootstrapServers,
                "group.id", "migration",
                "enable.auto.commit", "false",
                "key.deserializer", StringDeserializer.class.getName(),
                "value.deserializer", StringDeserializer.class.getName(),
                AUTO_OFFSET_RESET_CONFIG, "ignore"
        );

        { // emulate previous, non-bg consumer
            var props = new HashMap<>(connectionProperties);
            props.put(AUTO_OFFSET_RESET_CONFIG, "earliest");
            try (var consumer = new KafkaConsumer<>(props)) {
                consumer.subscribe(List.of(TOPIC_NAME));
                var records = consumer.poll(POLL_TIMEOUT);
                assertEquals(3, records.count());
                // commit only first record
                var first = records.iterator().next();
                var commit = Map.of(new TopicPartition(first.topic(), first.partition()), new OffsetAndMetadata(first.offset() + 1));
                consumer.commitSync(commit);
            }
        }

        // create bg consumer, that should handle existing offset and migrate bg offset version
        try (BGKafkaConsumerImpl<String, String> consumer = new BGKafkaConsumerImpl<>(BGKafkaConsumerConfig.builder(connectionProperties,
                        "orders", M2M_TOKEN_SUPPLIER, new InMemoryBlueGreenStatePublisher(Env.namespace()))
                .deserializers(new StringDeserializer(), new StringDeserializer()).build())) {
            // explicitly define types to prevent generics misconfigurations
            RecordsBatch<String, String> records = consumer.poll(POLL_TIMEOUT).get();
            assertEquals(2, records.getBatch().size());
            assertEquals(records.getCommitMarker(), records.getBatch().get(1).getCommitMarker());

            assertEquals("<order2>", records.getBatch().get(0).getConsumerRecord().value());
            assertEquals("<order3>", records.getBatch().get(1).getConsumerRecord().value());
        }
    }

    @Test
    void testCandidate() {
        var connectionProperties = Map.<String, Object>of(
                "bootstrap.servers", bootstrapServers,
                "group.id", "order-proc",
                "enable.auto.commit", "false");

        var v1 = new Version("v1");
        var v2 = new Version("v2");

        OffsetDateTime updateTime = OffsetDateTime.parse("2018-12-03T19:34:50Z");
        BlueGreenState bgState1 = new BlueGreenState(
                new NamespaceVersion(NAMESPACE_1, State.ACTIVE, v1),
                new NamespaceVersion(NAMESPACE_2, State.CANDIDATE, v2),
                updateTime);

        BlueGreenState bgState2 = new BlueGreenState(
                new NamespaceVersion(NAMESPACE_2, State.CANDIDATE, v2),
                new NamespaceVersion(NAMESPACE_1, State.ACTIVE, v1),
                updateTime);

        var statePublisherActive = new InMemoryBlueGreenStatePublisher(bgState1);
        var statePublisherCandidate = new InMemoryBlueGreenStatePublisher(bgState2);

        RecordsBatch<String, String> records;
        try (BGKafkaConsumerImpl<String, String> consumerActive = new BGKafkaConsumerImpl<>(BGKafkaConsumerConfig.builder(connectionProperties,
                        TOPIC_NAME, M2M_TOKEN_SUPPLIER, statePublisherActive)
                .deserializers(new StringDeserializer(), new StringDeserializer())
                .consistencyMode(ConsumerConsistencyMode.GUARANTEE_CONSUMPTION).build());
             BGKafkaConsumerImpl<String, String> consumerCandidate = new BGKafkaConsumerImpl<>(BGKafkaConsumerConfig.builder(connectionProperties,
                             TOPIC_NAME, M2M_TOKEN_SUPPLIER, statePublisherCandidate)
                     .deserializers(new StringDeserializer(), new StringDeserializer())
                     .consistencyMode(ConsumerConsistencyMode.GUARANTEE_CONSUMPTION)
                     .candidateOffsetSetupStrategy(OffsetSetupStrategy.LATEST).build())) {
            log.info(" =========================================================================");
            log.info(" get records by active version and commit only first record");
            log.info(" =========================================================================");

            // explicitly define types to prevent generics misconfigurations
            records = consumerActive.poll(POLL_TIMEOUT).get();
            assertEquals(3, records.getBatch().size());
            assertEquals(records.getCommitMarker(), records.getBatch().get(2).getCommitMarker());

            String recordData = records.getBatch().get(0).getConsumerRecord().value();
            assertEquals("<order1>", recordData);

            // commit only first record offset
            consumerActive.commitSync(records.getBatch().get(0).getCommitMarker());
            dumpOffsets("order-proc", "orders");

            log.info(" =========================================================================");
            log.info(" construct candidate consumer");
            log.info(" =========================================================================");

            // no records because of defined initial offset policy
            assertTrue(consumerCandidate.poll(POLL_TIMEOUT).isEmpty());
            dumpOffsets("order-proc", "orders");

            log.info(" =========================================================================");
            log.info(" add some more records");
            log.info(" =========================================================================");

            try (KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps)) {
                producer.send(new ProducerRecord<>(TOPIC_NAME, 0, "4", "<order4>", v1headers));
                producer.send(new ProducerRecord<>(TOPIC_NAME, 0, "5", "<order5>", v2headers));
                producer.send(new ProducerRecord<>(TOPIC_NAME, 0, "6", "<order6>"));
            }

            log.info(" =========================================================================");
            log.info(" promote ");
            log.info(" =========================================================================");

            updateTime = OffsetDateTime.parse("2018-12-05T19:34:50Z");
            bgState1 = new BlueGreenState(
                    new NamespaceVersion(NAMESPACE_1, State.LEGACY, v1),
                    new NamespaceVersion(NAMESPACE_2, State.ACTIVE, v2),
                    updateTime);

            bgState2 = new BlueGreenState(
                    new NamespaceVersion(NAMESPACE_2, State.ACTIVE, v2),
                    new NamespaceVersion(NAMESPACE_1, State.LEGACY, v1),
                    updateTime);

            statePublisherActive.setBlueGreenState(bgState1);
            statePublisherCandidate.setBlueGreenState(bgState2);

            log.info(" =========================================================================");
            log.info(" poll by legacy consumer ");
            log.info(" =========================================================================");
            records = consumerActive.poll(POLL_TIMEOUT).get();
            dumpOffsets("order-proc", "orders");
            assertEquals(1, records.getBatch().size());
            assertEquals("4", records.getBatch().get(0).getConsumerRecord().key());

            log.info(" =========================================================================");
            log.info(" poll by active consumer ");
            log.info(" =========================================================================");
            records = consumerCandidate.poll(POLL_TIMEOUT).get();
            dumpOffsets("order-proc", "orders");
            assertEquals(4, records.getBatch().size());
            assertEquals("2", records.getBatch().get(0).getConsumerRecord().key());
            assertEquals("3", records.getBatch().get(1).getConsumerRecord().key());
            assertEquals("5", records.getBatch().get(2).getConsumerRecord().key());
            assertEquals("6", records.getBatch().get(3).getConsumerRecord().key());
        }
    }

    @Test
    void testPartitionedTopicOffsetMarker() throws Exception {
        var PRICES_TOPIC = "prices";
        admin.createTopics(List.of(new NewTopic(PRICES_TOPIC, 2, (short) 1)));

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps)) {
            producer.send(new ProducerRecord<>(PRICES_TOPIC, 0, "1", "discount1"));
            producer.send(new ProducerRecord<>(PRICES_TOPIC, 1, "2", "discount2"));
            producer.send(new ProducerRecord<>(PRICES_TOPIC, 1, "3", "discount3"));
        }

        var connectionProperties = Map.<String, Object>of(
                BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                GROUP_ID_CONFIG, "test-marker-prices",
                ENABLE_AUTO_COMMIT_CONFIG, "false",
                AUTO_OFFSET_RESET_CONFIG, "earliest",
                KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class
        );

        var trackerActive = new InMemoryBlueGreenStatePublisher(NAMESPACE_1);

        // active version
        try (BGKafkaConsumerImpl<String, String> consumerActive = new BGKafkaConsumerImpl<>(BGKafkaConsumerConfig.builder(connectionProperties,
                        PRICES_TOPIC, M2M_TOKEN_SUPPLIER, trackerActive)
                .deserializers(new StringDeserializer(), new StringDeserializer()).build())) {
            // explicitly define types to prevent generics misconfigurations
            var records = consumerActive.poll(POLL_TIMEOUT).get();
            assertEquals(3, records.getBatch().size());
            assertEquals(
                    Map.of(
                            new TopicPartition(PRICES_TOPIC, 0), new OffsetAndMetadata(1),
                            new TopicPartition(PRICES_TOPIC, 1), new OffsetAndMetadata(2)
                    ),
                    records.getCommitMarker().getPosition());
        }
    }

    private void dumpOffsets(String groupIdPrefix, String topic) {
        var indexer = new OffsetsIndexer(groupIdPrefix, new AdminAdapterImpl(() -> Admin.create(Map.<String, Object>of("bootstrap.servers", bootstrapServers))));

        System.out.println(">>>> Offsets: ");
        indexer.dump((k, v) -> System.out.printf("%s ->\n%s\n", k,
                String.join("\n", v.entrySet().stream().map(entry -> String.format("%s=%d", entry.getKey(), entry.getValue().offset()))
                        .sorted().toList())
        ));
    }

    private void dumpOffsets(String groupIdPrefix, Set<String> topics, Map<String, Object> properties) {
        var indexer = new OffsetsIndexer(groupIdPrefix, new AdminAdapterImpl(() -> Admin.create(Map.<String, Object>of("bootstrap.servers", bootstrapServers))));

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);
        List<TopicPartition> topicPartitions = topics.stream().flatMap(topic ->
                consumer.partitionsFor(topic).stream().map(p -> new TopicPartition(topic, p.partition()))
        ).toList();

        String info = consumer.endOffsets(topicPartitions).entrySet().stream()
                .map(entry -> String.format("%s=%d", entry.getKey(), entry.getValue()))
                .sorted().collect(Collectors.joining("\n"));
        System.out.printf(">>>> End Offsets:\n%s\n", info);

        System.out.println(">>>> Current Offsets: ");
        indexer.dump((k, v) -> System.out.printf("%s ->\n%s\n", k,
                String.join("\n", v.entrySet().stream().map(entry -> String.format("%s=%d", entry.getKey(), entry.getValue().offset()))
                        .sorted().toList())
        ));
    }
}