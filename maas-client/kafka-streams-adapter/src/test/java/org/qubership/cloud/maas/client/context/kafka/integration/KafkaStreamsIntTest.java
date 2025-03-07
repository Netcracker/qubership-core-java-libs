package org.qubership.cloud.maas.client.context.kafka.integration;

import org.qubership.cloud.maas.client.api.Classifier;
import org.qubership.cloud.maas.client.api.kafka.KafkaMaaSClient;
import org.qubership.cloud.maas.client.api.kafka.TopicCreateOptions;
import org.qubership.cloud.maas.client.api.kafka.protocolextractors.OnTopicExists;
import org.qubership.cloud.maas.client.context.kafka.MaaSKafkaAdminWrapper;
import org.qubership.cloud.maas.client.context.kafka.MaaSKafkaClientSupplier;
import org.qubership.cloud.maas.client.impl.Env;
import org.qubership.cloud.maas.client.impl.dto.kafka.v1.TopicInfo;
import org.qubership.cloud.maas.client.impl.dto.kafka.v1.TopicSettings;
import org.qubership.cloud.maas.client.impl.kafka.TopicAddressImpl;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.ForwardingAdmin;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junitpioneer.jupiter.SetSystemProperty;
import org.mockito.Mockito;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j
@Testcontainers
class KafkaStreamsIntTest {

    static String TEXT_TOPIC = "lines";
    static String WORDS_TOPIC = "words";
    static String COUNTS_STORE_NAME = "counts.store";
    static String APP_ID = "words.count.app";

    @Container
    KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0")
            .asCompatibleSubstituteFor("confluentinc/cp-kafka")).withKraft();

    Admin admin;
    String bootstrapServers;

    @BeforeEach
    void setupKafka() {
        bootstrapServers = kafkaContainer.getBootstrapServers();
        Properties props = new Properties();
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        admin = Admin.create(props);
    }

    @AfterEach
    void cleanup() {
        Optional.ofNullable(admin).ifPresent(Admin::close);
    }

    @Test
    @SetSystemProperty(key = Env.PROP_NAMESPACE, value = "test-namespace")
    @Timeout(value = 2, unit = TimeUnit.MINUTES)
    void testStreams() {
        admin.createTopics(List.of(
                new NewTopic(TEXT_TOPIC, 1, (short) 1),
                new NewTopic(WORDS_TOPIC, 1, (short) 1)
        ));
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.CLIENT_ID_CONFIG, "lines-submitter");
        producerProps.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps)) {
            Map.of(
                            "1", "word1",
                            "2", "word2 word2",
                            "3", "word3 word3 word3")
                    .forEach((key, sentence) -> {
                        log.info("Sending record '{}':'{}' to '{}' topic", key, sentence, TEXT_TOPIC);
                        producer.send(new ProducerRecord<>(TEXT_TOPIC, 0, key, sentence));
                    });
        }

        KafkaMaaSClient kafkaMaaSClient = Mockito.mock(KafkaMaaSClient.class);
        Mockito.when(kafkaMaaSClient.getTopic(Mockito.any())).thenAnswer(i -> {
            Classifier classifier = i.getArgument(0, Classifier.class);
            Set<String> topics = admin.listTopics().names().get();
            String topicName = classifier.getName();
            if (topics.contains(topicName)) {
                // populate fields required for MaaSKafkaAdminWrapper
                TopicInfo topicInfo = new TopicInfo();
                topicInfo.setName(topicName);
                return Optional.of(new TopicAddressImpl(topicInfo));
            }
            return Optional.empty();
        });

        Mockito.when(kafkaMaaSClient.getOrCreateTopic(Mockito.any(), Mockito.any())).thenAnswer(i -> {
            Classifier classifier = i.getArgument(0, Classifier.class);
            TopicCreateOptions topicCreateOptions = i.getArgument(1, TopicCreateOptions.class);
            String topicName = topicCreateOptions.getName();
            Assertions.assertEquals("test-namespace", classifier.getNamespace());
            int numPartitions = topicCreateOptions.getNumPartitions();
            waitTopicCreated(admin.createTopics(List.of(new NewTopic(topicName, numPartitions, (short) 1))).values());
            // populate fields required for MaaSKafkaAdminWrapper
            TopicInfo topicInfo = new TopicInfo();
            topicInfo.setName(topicName);
            TopicSettings actualSettings = new TopicSettings();
            actualSettings.setNumPartitions(numPartitions);
            topicInfo.setActualSettings(actualSettings);
            return new TopicAddressImpl(topicInfo);
        });

        Properties kafkaStreamProps = new Properties();
        kafkaStreamProps.put(StreamsConfig.APPLICATION_ID_CONFIG, APP_ID);
        kafkaStreamProps.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        kafkaStreamProps.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        kafkaStreamProps.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, String> textLines = builder.stream(TEXT_TOPIC);
        KTable<String, Long> wordCounts = textLines
                .flatMapValues(textLine -> Arrays.asList(textLine.toLowerCase().split("\\W+")))
                .groupBy((key, word) -> word)
                .count(Materialized.as(COUNTS_STORE_NAME));
        wordCounts.toStream().to(WORDS_TOPIC, Produced.with(Serdes.String(), Serdes.Long()));

        Function<Map<String, Object>, ForwardingAdmin> adminSupplier = config -> MaaSKafkaAdminWrapper.builder(config, kafkaMaaSClient).build();
        MaaSKafkaClientSupplier clientSupplier = new MaaSKafkaClientSupplier(adminSupplier);
        try (KafkaStreams streams = new KafkaStreams(builder.build(), kafkaStreamProps, clientSupplier)) {
            streams.cleanUp(); // cleanup local cache for this test to be able to re-run on local envs
            streams.start();
            Properties consumerProps = new Properties();
            consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "words-counter-consumer");
            consumerProps.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class.getName());
            try (KafkaConsumer<String, Long> kafkaConsumer = new KafkaConsumer<>(consumerProps)) {
                kafkaConsumer.subscribe(List.of(WORDS_TOPIC));
                List<String> result = new ArrayList<>();
                while (result.size() != 3) {
                    ConsumerRecords<String, Long> poll = kafkaConsumer.poll(Duration.ofSeconds(1));
                    poll.records(WORDS_TOPIC).forEach(record -> {
                        log.info("Received record '{}':'{}' from '{}' topic", record.key(), record.value(), WORDS_TOPIC);
                        result.add(record.key());
                    });
                }
                Collections.sort(result);
                List<String> expected = List.of("word1", "word2", "word3");
                Assertions.assertEquals(expected, result);
            }
        }
    }

    @Test
    void testCustomFuncs() {
        String testTopic = "test-topic";
        KafkaMaaSClient kafkaMaaSClient = Mockito.mock(KafkaMaaSClient.class);

        Mockito.when(kafkaMaaSClient.getTopic(Mockito.any())).thenAnswer(i -> {
            Classifier classifier = i.getArgument(0, Classifier.class);
            Set<String> topics = admin.listTopics().names().get();
            String topicName = classifier.getName();
            if (topics.contains(topicName)) {
                // populate fields required for MaaSKafkaAdminWrapper
                TopicInfo topicInfo = new TopicInfo();
                topicInfo.setName(topicName);
                return Optional.of(new TopicAddressImpl(topicInfo));
            }
            return Optional.empty();
        });

        Mockito.when(kafkaMaaSClient.getOrCreateTopic(Mockito.any(), Mockito.any())).thenAnswer(i -> {
            Classifier classifier = i.getArgument(0, Classifier.class);
            TopicCreateOptions topicCreateOptions = i.getArgument(1, TopicCreateOptions.class);
            String topicName = topicCreateOptions.getName();

            Assertions.assertEquals(testTopic, classifier.getName());
            Assertions.assertEquals("test-namespace", classifier.getNamespace());
            Assertions.assertEquals(Optional.of("test-tenantId"), classifier.getTenantId());
            Assertions.assertEquals(OnTopicExists.MERGE, topicCreateOptions.getOnTopicExists());

            int numPartitions = topicCreateOptions.getNumPartitions();

            waitTopicCreated(admin.createTopics(List.of(new NewTopic(topicName, numPartitions, (short) 1))).values());

            TopicInfo topicInfo = new TopicInfo();
            topicInfo.setName(topicName);
            TopicSettings actualSettings = new TopicSettings();
            actualSettings.setNumPartitions(numPartitions);
            topicInfo.setActualSettings(actualSettings);
            return new TopicAddressImpl(topicInfo);

        });
        Function<Map<String, Object>, ForwardingAdmin> adminSupplier = config -> MaaSKafkaAdminWrapper.builder(config, kafkaMaaSClient)
                .classifierSupplier(topicName ->
                        new Classifier(topicName, Classifier.NAMESPACE, "test-namespace", Classifier.TENANT_ID, "test-tenantId"))
                .onTopicExistsAction(OnTopicExists.MERGE)
                .build();
        MaaSKafkaClientSupplier clientSupplier = new MaaSKafkaClientSupplier(adminSupplier);
        Admin admin = clientSupplier.getAdmin(Map.of(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers));
        admin.createTopics(List.of(new NewTopic(testTopic, Optional.of(1), Optional.of((short) 1))));
    }


    @Test
    void testTopicPresentInKafkaButNotPresentInMaaS() throws Exception {
        KafkaMaaSClient kafkaMaaSClient = Mockito.mock(KafkaMaaSClient.class);

        // create topic so it will be present in Kafka
        String testTopic = "test-topic-not-present-in-maas";
        admin.createTopics(List.of(new NewTopic(testTopic, 1, (short) 1)));

        Mockito.when(kafkaMaaSClient.getTopic(Mockito.any())).thenAnswer(i -> {
            Classifier classifier = i.getArgument(0, Classifier.class);
            Set<String> topics = admin.listTopics().names().get();
            String topicName = classifier.getName();
            Assertions.assertTrue(topics.contains(topicName));
            // return Optional empty to imitate the topic is not registered in MaaS
            return Optional.empty();
        });

        Function<Map<String, Object>, ForwardingAdmin> adminSupplier = config -> MaaSKafkaAdminWrapper.builder(config, kafkaMaaSClient)
                .classifierSupplier(topicName -> new Classifier(topicName, Classifier.NAMESPACE, "test-namespace"))
                .build();
        MaaSKafkaClientSupplier clientSupplier = new MaaSKafkaClientSupplier(adminSupplier);
        Admin admin = clientSupplier.getAdmin(Map.of(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers));
        Map<String, KafkaFuture<TopicDescription>> topicNames = admin.describeTopics(List.of(testTopic)).topicNameValues();

        Assertions.assertThrows(ExecutionException.class, () -> {
            topicNames.get(testTopic).get();
        }, String.format("Topic %s not found in MaaS by classifier %s", testTopic, new Classifier(testTopic, Classifier.NAMESPACE, "test-namespace")));
    }

    @SneakyThrows
    private void waitTopicCreated(Map<String, KafkaFuture<Void>> futures) {
        futures.values().forEach(future -> {
            try {
                future.get(30, TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
