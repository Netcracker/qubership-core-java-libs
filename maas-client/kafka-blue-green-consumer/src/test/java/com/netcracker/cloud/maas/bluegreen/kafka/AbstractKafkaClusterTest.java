package org.qubership.cloud.maas.bluegreen.kafka;

import org.qubership.cloud.bluegreen.api.model.BlueGreenState;
import org.qubership.cloud.bluegreen.api.model.NamespaceVersion;
import org.qubership.cloud.bluegreen.api.model.State;
import org.qubership.cloud.bluegreen.api.model.Version;
import org.qubership.cloud.bluegreen.impl.service.InMemoryBlueGreenStatePublisher;
import org.qubership.cloud.maas.bluegreen.kafka.util.KafkaContainerCluster;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.qubership.cloud.framework.contexts.xversion.XVersionContextObject.X_VERSION_SERIALIZATION_NAME;
import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;

@Slf4j
@Testcontainers
abstract class AbstractKafkaClusterTest {
    static String ORIGIN_NS = "origin";
    static String PEER_NS = "peer";

    static int brokers = 3;
    static int pods = 3;
    static int partitions = pods * 2;
    static short replicationFactor = 2;
    static int executorSize = pods * 2;
    static ExecutorService executor = Executors.newFixedThreadPool(executorSize);
    static Comparator<TopicPartition> topicPartitionComp = Comparator.comparing(TopicPartition::topic).thenComparing(TopicPartition::partition);

    Duration POLL_TIMEOUT = Duration.ofSeconds(30);
    Duration NO_RECORDS_TIMEOUT = Duration.ofSeconds(5);

    KafkaContainerCluster cluster;

    Admin admin;
    String bootstrapServers;
    Properties producerProps;

    @BeforeEach
    void setupKafka() {
        cluster = new KafkaContainerCluster("7.4.0", brokers, replicationFactor, false);
        cluster.start();
        bootstrapServers = cluster.getBootstrapServers();
        log.info("BootstrapServers: {}", bootstrapServers);
        Properties props = new Properties();
        props.put(BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        admin = Admin.create(props);

        producerProps = new Properties();
        producerProps.putAll(Map.of(ProducerConfig.CLIENT_ID_CONFIG, "test-producer",
                BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName(),
                VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName()));
    }

    @AfterEach
    void stopKafka() {
        Optional.ofNullable(admin).ifPresent(Admin::close);
        cluster.stop();
    }

    static Map<TopicPartition, Long> offsetsMap(Set<String> topics, long expectedLatestOffset) {
        return topics.stream().flatMap(topic -> IntStream.range(0, partitions).boxed().map(p -> new TopicPartition(topic, p)))
                .collect(Collectors.toMap(tp -> tp, tp -> expectedLatestOffset));
    }

    record States(BlueGreenState origin, BlueGreenState peer) {
    }

    record StateVersion(State state, String version) {
    }

    record ConsumerKey<K, V>(String key, Collection<? extends BGKafkaConsumer<K, V>> consumers) {
    }

    static States states(String updateTime, StateVersion origin, StateVersion... peer) {
        Optional<StateVersion> peerOpt = Optional.ofNullable(peer.length > 0 ? peer[0] : null);
        BlueGreenState originBgState = new BlueGreenState(
                new NamespaceVersion(ORIGIN_NS, origin.state, new Version(origin.version)),
                peerOpt.flatMap(stateVersion -> Optional.of(new NamespaceVersion(PEER_NS, stateVersion.state,
                        stateVersion.version == null ? null : new Version(stateVersion.version)))),
                OffsetDateTime.parse(updateTime));
        BlueGreenState peerBgState = originBgState.getSibling().isPresent() ? new BlueGreenState(
                originBgState.getSibling().get(),
                originBgState.getCurrent(),
                originBgState.getUpdateTime()) : null;
        return new States(originBgState, peerBgState);
    }

    <K, V> List<ProducerRecord<K, V>> sendRecords(String logMessage, String version, Collection<ProducerRecord<K, V>> records) {
        log.info(logMessage + ", version: {}", version);
        try (KafkaProducer<K, V> producer = new KafkaProducer<>(producerProps)) {
            return records.stream().map(record -> {
                if (version != null && !version.isBlank()) {
                    record.headers().add(new RecordHeader(X_VERSION_SERIALIZATION_NAME, new Version(version).value().getBytes()));
                }
                log.info("producing: ProducerRecord(topic={}, partition={}, key={}, value={}, headers=[{}])",
                        record.topic(), record.partition(), record.key(), record.value(),
                        String.join(", ", Arrays.stream(record.headers().toArray())
                                .map(h -> String.format("%s:%s", h.key(), new String(h.value()))).toList()));
                try {
                    producer.send(record).get(10, TimeUnit.SECONDS);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return record;
            }).toList();
        }
    }

    <K, V> Optional<RecordsBatch<K, V>> consumeAndCommit(Duration timeout, BGKafkaConsumer<K, V> consumer) {
        Optional<RecordsBatch<K, V>> batchOpt = consumer.poll(timeout);
        if (batchOpt.isEmpty()) {
            return Optional.empty();
        }
        RecordsBatch<K, V> batch = batchOpt.get();
        CommitMarker commitMarker = batch.getCommitMarker();
        consumer.commitSync(commitMarker);
        return batchOpt;
    }

    @SneakyThrows
    boolean parallelConsume(Duration timeout,
                            Map<String, Set<String>> expectedRecords,
                            Map<String, Map<TopicPartition, Long>> expectedOffsets,
                            ConsumerKey<String, String>... consumers) {
        if (Arrays.stream(consumers).map(ConsumerKey::consumers).toList().size() > executorSize) {
            throw new IllegalArgumentException(String.format("consumers size: '%d' must be less or equal to the executor's size: '%d'", consumers.length, executorSize));
        }
        expectedRecords = expectedRecords.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> new TreeSet<>(entry.getValue()),
                (v1, v2) -> new TreeSet<>(Stream.concat(v1.stream(), v2.stream()).collect(Collectors.toSet())), TreeMap::new));
        expectedOffsets = expectedOffsets.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, (Map.Entry<String, Map<TopicPartition, Long>> entry) -> {
                    TreeMap<TopicPartition, Long> map = new TreeMap<>(topicPartitionComp);
                    map.putAll(entry.getValue());
                    return map;
                },
                (v1, v2) -> {
                    TreeMap<TopicPartition, Long> map = new TreeMap<>(topicPartitionComp);
                    map.putAll(Stream.concat(v1.entrySet().stream(), v2.entrySet().stream()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
                    return map;
                }, TreeMap::new));
        Map<String, SortedSet<String>> receivedRecords = new ConcurrentHashMap<>(expectedRecords.size());
        Map<String, Map<TopicPartition, Long>> receivedOffsets = new ConcurrentHashMap<>(expectedOffsets.size());
        int attempts = 5;
        int attempt = 0;
        while (attempt++ < attempts) {
            log.info("parallel consume, attempt #{}", attempt);
            Duration consumeTimeout = Duration.ofMillis(timeout.toMillis() / attempts);
            Arrays.stream(consumers).forEach(entry -> entry.consumers().stream()
                    .map(consumer -> executor.submit(() -> consumeAndCommit(consumeTimeout, consumer)))
                    .toList().stream()
                    .forEach(future -> {
                        try {
                            // initiate maps with setting key
                            receivedRecords.putIfAbsent(entry.key(), new TreeSet<>());
                            Comparator<TopicPartition> comparator = topicPartitionComp;
                            receivedOffsets.putIfAbsent(entry.key(), new TreeMap<>(comparator));

                            Optional<RecordsBatch<String, String>> recordsBatchOpt = future.get();
                            if (recordsBatchOpt.isEmpty()) {
                                return;
                            }
                            RecordsBatch<String, String> recordsBatch = recordsBatchOpt.get();
                            Map<TopicPartition, Long> position = recordsBatch.getCommitMarker().getPosition().entrySet().stream()
                                    .collect(Collectors.toMap(Map.Entry::getKey, en -> en.getValue().offset()));
                            receivedOffsets.compute(entry.key(), (k, v) -> {
                                if (v == null) {
                                    v = position;
                                } else {
                                    v.putAll(position);
                                }
                                return v;
                            });
                            List<Record<String, String>> records = recordsBatch.getBatch();
                            SortedSet<String> recordsAsStrings = new TreeSet<>(recordsAsStrings(records, cRecordsAsString));
                            receivedRecords.compute(entry.key(), (k, v) -> {
                                if (v == null) {
                                    v = recordsAsStrings;
                                } else {
                                    v.addAll(recordsAsStrings);
                                }
                                return v;
                            });
                        } catch (Exception e) {
                            throw new IllegalStateException(e);
                        }
                    }));
            if (Objects.equals(receivedRecords, expectedRecords) && Objects.equals(receivedOffsets, expectedOffsets)) {
                // there are cases when even when we've read all expected messages, but still need one more poll() in order to read the latest offsets
                // (of those messages which were filtered out) so, we are checking offsets here as well
                return true;
            }
        }
        log.error("""
                        Failed to receive expected records.
                        expectedRecordsMap:{}
                        {}
                        receivedRecordsMap:{}
                        -
                        expectedOffsets: {}
                        {}
                        receivedOffsets: {}
                        """,
                expectedRecords, (Objects.equals(receivedRecords, expectedRecords) ? "=" : "!="), receivedRecords,
                expectedOffsets, (Objects.equals(receivedOffsets, expectedOffsets) ? "=" : "!="), receivedOffsets);
        return false;
    }

    void updatePublishersVersions(States states, InMemoryBlueGreenStatePublisher origin, InMemoryBlueGreenStatePublisher... peer) {
        origin.setBlueGreenState(states.origin);
        if (peer.length > 0) {
            peer[0].setBlueGreenState(states.peer);
        }
    }

    ProducerRecord<String, String> record(String topic, int partition, int msgId) {
        return new ProducerRecord<>(topic, partition, buildKey(msgId), buildValue(topic, msgId));
    }

    static <T> Set<String> recordsAsStrings(Collection<T> records, Function<T, String> toString) {
        return new LinkedHashSet<>(Optional.ofNullable(records).orElse(Set.of()).stream().map(toString).sorted().toList());
    }

    static Function<ProducerRecord<String, String>, String> pRecordsAsString = AbstractKafkaClusterTest::recordAsString;
    static Function<Record<String, String>, String> cRecordsAsString = AbstractKafkaClusterTest::recordAsString;

    static String recordAsString(ProducerRecord<String, String> record) {
        return recordAsString(record.topic(), record.partition(), record.key(), record.value());
    }

    static String recordAsString(Record<String, String> record) {
        ConsumerRecord<String, String> r = record.getConsumerRecord();
        return recordAsString(r.topic(), r.partition(), r.key(), r.value());
    }

    static String recordAsString(String topic, int partition, String key, String value) {
        return String.format("%s/%d/%s/%s", topic, partition, key, value);
    }

    static String buildValue(String topic, int msgId) {
        return String.format("%s-%04d", topic, msgId);
    }

    static String buildKey(int msgId) {
        return String.format("%04d", msgId);
    }

    static class ConsumersCloseable<K, V> implements AutoCloseable {
        Collection<BGKafkaConsumer<K, V>> consumers;

        public ConsumersCloseable(Collection<BGKafkaConsumer<K, V>> consumers) {
            this.consumers = consumers;
        }

        @Override
        public void close() {
            this.consumers.forEach(BGKafkaConsumer::close);
        }
    }

}