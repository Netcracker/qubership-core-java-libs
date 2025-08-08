package com.netcracker.cloud.maas.bluegreen.kafka;

import org.qubership.cloud.bluegreen.api.model.State;
import org.qubership.cloud.bluegreen.impl.service.InMemoryBlueGreenStatePublisher;
import org.qubership.cloud.maas.bluegreen.kafka.impl.BGKafkaConsumerImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.qubership.cloud.maas.bluegreen.kafka.impl.BGKafkaConsumerConfig.builder;
import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.*;

@Slf4j
class BGKafkaMultiTopicConsumerTest extends AbstractKafkaClusterTest {

    @Test
    void testMultiTopicConsumerGroup() {
        Set<String> topics = Set.of("topic-1", "topic-2");

        admin.createTopics(topics.stream().map(topic -> new NewTopic(topic, partitions, replicationFactor)).toList()).values().values()
                .forEach(future -> {
                    try {
                        future.get();
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                });

        String multiTopicGroup = "multi-topic-group";
        var connectionProperties = Map.<String, Object>of(
                BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                GROUP_ID_CONFIG, multiTopicGroup,
                ENABLE_AUTO_COMMIT_CONFIG, "false",
                KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class
        );

        States states = states("2024-01-01T10:00:00Z", new StateVersion(State.ACTIVE, "v1"));

        var statePublisherOrigin = new InMemoryBlueGreenStatePublisher(states.origin());

        Collection<BGKafkaConsumerImpl<String, String>> originConsumersPods = IntStream.range(0, pods).boxed().map(i ->
                new BGKafkaConsumerImpl<String, String>(builder(connectionProperties, topics, statePublisherOrigin)
                        .deserializers(new StringDeserializer(), new StringDeserializer())
                        .consistencyMode(ConsumerConsistencyMode.GUARANTEE_CONSUMPTION)
                        .build())).toList();
        AtomicInteger msgCounter = new AtomicInteger(0);
        try (ConsumersCloseable originConsumersCloseable = new ConsumersCloseable(originConsumersPods)) {
            Assertions.assertTrue(parallelConsume(NO_RECORDS_TIMEOUT,
                    Map.of(ORIGIN_NS, Set.of()),
                    Map.of(ORIGIN_NS, Map.of()),
                    new ConsumerKey<>(ORIGIN_NS, originConsumersPods)));

            List<ProducerRecord<String, String>> sentRecords = sendRecords("producing records for standalone active", null,
                    IntStream.range(0, partitions).boxed().flatMap(partition -> topics.stream().map(topic ->
                                    record(topic, partition, msgCounter.incrementAndGet())))
                            .toList());
            long expectedLatestOffset = sentRecords.size() / topics.size() / partitions;
            log.info("consume and commit all records by standalone active consumer");
            Assertions.assertTrue(parallelConsume(POLL_TIMEOUT,
                    Map.of(ORIGIN_NS, recordsAsStrings(sentRecords, pRecordsAsString)),
                    Map.of(ORIGIN_NS, offsetsMap(topics, expectedLatestOffset)),
                    new ConsumerKey<>(ORIGIN_NS, originConsumersPods)));

            log.info("""
                    BG-Operation:init
                    """);
            states = states("2024-01-01T10:01:00Z", new StateVersion(State.ACTIVE, "v1"), new StateVersion(State.IDLE, null));
            updatePublishersVersions(states, statePublisherOrigin);

            Assertions.assertTrue(parallelConsume(NO_RECORDS_TIMEOUT,
                    Map.of(ORIGIN_NS, Set.of()),
                    Map.of(ORIGIN_NS, Map.of()),
                    new ConsumerKey<>(ORIGIN_NS, originConsumersPods)));

            sentRecords = sendRecords("producing records: active+idle", null,
                    IntStream.range(0, partitions).boxed()
                            .flatMap(partition -> topics.stream().map(topic ->
                                    record(topic, partition, msgCounter.incrementAndGet())))
                            .toList());
            expectedLatestOffset += sentRecords.size() / topics.size() / partitions;

            log.info("consume and commit all records by active consumer");
            Assertions.assertTrue(parallelConsume(POLL_TIMEOUT,
                    Map.of(ORIGIN_NS, recordsAsStrings(sentRecords, pRecordsAsString)),
                    Map.of(ORIGIN_NS, offsetsMap(topics, expectedLatestOffset)),
                    new ConsumerKey<>(ORIGIN_NS, originConsumersPods)));

            var statePublisherPeer = new InMemoryBlueGreenStatePublisher(states.peer());

            Collection<BGKafkaConsumerImpl<String, String>> peerConsumersPods = IntStream.range(0, pods).boxed().map(i ->
                    new BGKafkaConsumerImpl<String, String>(builder(connectionProperties, topics, statePublisherPeer)
                            .deserializers(new StringDeserializer(), new StringDeserializer())
                            .consistencyMode(ConsumerConsistencyMode.GUARANTEE_CONSUMPTION)
                            .build())).toList();
            try (ConsumersCloseable peerConsumersCloseable = new ConsumersCloseable(peerConsumersPods)) {
                int bgCycles = 1;
                int timeCounter = 2;
                int verCounter = 1;
                for (int i = 1; i <= bgCycles; i++) {
                    // warmup#1
                    String ver1Active = String.format("v%d", verCounter);
                    String ver1Candidate = String.format("v%d", ++verCounter);
                    // promote#1
                    // rollback#1
                    // promote#2
                    // commit#1
                    String ver2Active = ver1Candidate;
                    String ver2Legacy = ver1Active;
                    // warmup#2
                    String ver3Active = ver2Active;
                    String ver3Candidate = String.format("v%d", ++verCounter);
                    // promote#1
                    String ver4Active = ver3Candidate;
                    String ver4Legacy = ver3Active;

                    log.info("""
                            BG-Operation:warmup #{}
                            """, i);
                    states = states(String.format("2024-01-01T10:%02d:00Z", ++timeCounter), new StateVersion(State.ACTIVE, ver1Active), new StateVersion(State.CANDIDATE, ver1Candidate));

                    updatePublishersVersions(states, statePublisherOrigin, statePublisherPeer);

                    Assertions.assertTrue(parallelConsume(NO_RECORDS_TIMEOUT,
                            Map.of(ORIGIN_NS, Set.of(), PEER_NS, Set.of()),
                            Map.of(ORIGIN_NS, Map.of(), PEER_NS, Map.of()),
                            new ConsumerKey<>(ORIGIN_NS, originConsumersPods),
                            new ConsumerKey<>(PEER_NS, peerConsumersPods)));

                    log.info("consume and commit all records by active, candidate consumers");
                    List<ProducerRecord<String, String>> sentRecordsV1Active = sendRecords("producing records for active", ver1Active,
                            IntStream.range(0, partitions).boxed()
                                    .flatMap(partition -> topics.stream().map(topic ->
                                            record(topic, partition, msgCounter.incrementAndGet())))
                                    .toList());
                    expectedLatestOffset += sentRecordsV1Active.size() / topics.size() / partitions;

                    List<ProducerRecord<String, String>> sentRecordsV1Candidate = sendRecords("producing records for candidate", ver1Candidate,
                            IntStream.range(0, partitions).boxed()
                                    .flatMap(partition -> topics.stream().map(topic ->
                                            record(topic, partition, msgCounter.incrementAndGet())))
                                    .toList());
                    expectedLatestOffset += sentRecordsV1Candidate.size() / topics.size() / partitions;

                    Assertions.assertTrue(parallelConsume(POLL_TIMEOUT,
                            Map.of(ORIGIN_NS, recordsAsStrings(sentRecordsV1Active, pRecordsAsString), PEER_NS, recordsAsStrings(sentRecordsV1Candidate, pRecordsAsString)),
                            Map.of(ORIGIN_NS, offsetsMap(topics, expectedLatestOffset), PEER_NS, offsetsMap(topics, expectedLatestOffset)),
                            new ConsumerKey<>(ORIGIN_NS, originConsumersPods),
                            new ConsumerKey<>(PEER_NS, peerConsumersPods)));

                    // ----------------------------------------

                    log.info("""
                            BG-Operation:promote #{}
                            """, i);
                    states = states(String.format("2024-01-01T10:%02d:00Z", ++timeCounter), new StateVersion(State.LEGACY, ver2Legacy), new StateVersion(State.ACTIVE, ver2Active));

                    updatePublishersVersions(states, statePublisherOrigin, statePublisherPeer);

                    log.info("consume and commit all records by active, legacy consumers");

                    Assertions.assertTrue(parallelConsume(NO_RECORDS_TIMEOUT,
                            Map.of(ORIGIN_NS, Set.of(), PEER_NS, Set.of()),
                            Map.of(ORIGIN_NS, Map.of(), PEER_NS, Map.of()),
                            new ConsumerKey<>(ORIGIN_NS, originConsumersPods),
                            new ConsumerKey<>(PEER_NS, peerConsumersPods)));

                    List<ProducerRecord<String, String>> sentRecordsV2Active = sendRecords("producing records for active", ver2Active,
                            IntStream.range(0, partitions).boxed()
                                    .flatMap(partition -> topics.stream().map(topic ->
                                            record(topic, partition, msgCounter.incrementAndGet())))
                                    .toList());
                    expectedLatestOffset += sentRecordsV2Active.size() / topics.size() / partitions;

                    List<ProducerRecord<String, String>> sentRecordsV2Legacy = sendRecords("producing records for legacy", ver2Legacy,
                            IntStream.range(0, partitions).boxed()
                                    .flatMap(partition -> topics.stream().map(topic ->
                                            record(topic, partition, msgCounter.incrementAndGet())))
                                    .toList());
                    expectedLatestOffset += sentRecordsV2Legacy.size() / topics.size() / partitions;

                    Assertions.assertTrue(parallelConsume(POLL_TIMEOUT,
                            Map.of(ORIGIN_NS, recordsAsStrings(sentRecordsV2Legacy, pRecordsAsString), PEER_NS, recordsAsStrings(sentRecordsV2Active, pRecordsAsString)),
                            Map.of(ORIGIN_NS, offsetsMap(topics, expectedLatestOffset), PEER_NS, offsetsMap(topics, expectedLatestOffset)),
                            new ConsumerKey<>(ORIGIN_NS, originConsumersPods),
                            new ConsumerKey<>(PEER_NS, peerConsumersPods)));

                    // ----------------------------------------

                    log.info("""
                            BG-Operation:rollback #{}
                            """, i);
                    states = states(String.format("2024-01-01T10:%02d:00Z", ++timeCounter), new StateVersion(State.ACTIVE, ver1Active), new StateVersion(State.CANDIDATE, ver1Candidate));

                    updatePublishersVersions(states, statePublisherOrigin, statePublisherPeer);

                    log.info("consume and commit all records by active, candidate consumers");

                    Assertions.assertTrue(parallelConsume(NO_RECORDS_TIMEOUT,
                            Map.of(ORIGIN_NS, Set.of(), PEER_NS, Set.of()),
                            Map.of(ORIGIN_NS, Map.of(), PEER_NS, Map.of()),
                            new ConsumerKey<>(ORIGIN_NS, originConsumersPods),
                            new ConsumerKey<>(PEER_NS, peerConsumersPods)));

                    sentRecordsV1Active = sendRecords("producing records for active", ver1Active,
                            IntStream.range(0, partitions).boxed()
                                    .flatMap(partition -> topics.stream().map(topic ->
                                            record(topic, partition, msgCounter.incrementAndGet())))
                                    .toList());
                    expectedLatestOffset += sentRecordsV1Active.size() / topics.size() / partitions;

                    sentRecordsV1Candidate = sendRecords("producing records for candidate", ver1Candidate,
                            IntStream.range(0, partitions).boxed()
                                    .flatMap(partition -> topics.stream().map(topic ->
                                            record(topic, partition, msgCounter.incrementAndGet())))
                                    .toList());
                    expectedLatestOffset += sentRecordsV1Candidate.size() / topics.size() / partitions;

                    Assertions.assertTrue(parallelConsume(POLL_TIMEOUT,
                            Map.of(ORIGIN_NS, recordsAsStrings(sentRecordsV1Active, pRecordsAsString), PEER_NS, recordsAsStrings(sentRecordsV1Candidate, pRecordsAsString)),
                            Map.of(ORIGIN_NS, offsetsMap(topics, expectedLatestOffset), PEER_NS, offsetsMap(topics, expectedLatestOffset)),
                            new ConsumerKey<>(ORIGIN_NS, originConsumersPods),
                            new ConsumerKey<>(PEER_NS, peerConsumersPods)));

                    // ----------------------------------------

                    log.info("""
                            BG-Operation:promote #{}
                            """, i + 1);
                    states = states(String.format("2024-01-01T10:%02d:00Z", ++timeCounter), new StateVersion(State.LEGACY, ver2Legacy), new StateVersion(State.ACTIVE, ver2Active));

                    updatePublishersVersions(states, statePublisherOrigin, statePublisherPeer);

                    log.info("consume and commit all records by active, legacy consumers");

                    Assertions.assertTrue(parallelConsume(NO_RECORDS_TIMEOUT,
                            Map.of(ORIGIN_NS, Set.of(), PEER_NS, Set.of()),
                            Map.of(ORIGIN_NS, Map.of(), PEER_NS, Map.of()),
                            new ConsumerKey<>(ORIGIN_NS, originConsumersPods),
                            new ConsumerKey<>(PEER_NS, peerConsumersPods)));

                    sentRecordsV2Active = sendRecords("producing records for active", ver2Active,
                            IntStream.range(0, partitions).boxed()
                                    .flatMap(partition -> topics.stream().map(topic ->
                                            record(topic, partition, msgCounter.incrementAndGet())))
                                    .toList());
                    expectedLatestOffset += sentRecordsV2Active.size() / topics.size() / partitions;

                    sentRecordsV2Legacy = sendRecords("producing records for legacy", ver2Legacy,
                            IntStream.range(0, partitions).boxed()
                                    .flatMap(partition -> topics.stream().map(topic ->
                                            record(topic, partition, msgCounter.incrementAndGet())))
                                    .toList());
                    expectedLatestOffset += sentRecordsV2Legacy.size() / topics.size() / partitions;

                    Assertions.assertTrue(parallelConsume(POLL_TIMEOUT,
                            Map.of(ORIGIN_NS, recordsAsStrings(sentRecordsV2Legacy, pRecordsAsString), PEER_NS, recordsAsStrings(sentRecordsV2Active, pRecordsAsString)),
                            Map.of(ORIGIN_NS, offsetsMap(topics, expectedLatestOffset), PEER_NS, offsetsMap(topics, expectedLatestOffset)),
                            new ConsumerKey<>(ORIGIN_NS, originConsumersPods),
                            new ConsumerKey<>(PEER_NS, peerConsumersPods)));

                    // ----------------------------------------

                    log.info("""
                            BG-Operation:commit #{}
                            """, i);
                    states = states(String.format("2024-01-01T10:%02d:00Z", ++timeCounter), new StateVersion(State.IDLE, null), new StateVersion(State.ACTIVE, ver2Active));

                    updatePublishersVersions(states, statePublisherOrigin, statePublisherPeer);


                    log.info("consume and commit all records by active consumer");

                    Assertions.assertTrue(parallelConsume(NO_RECORDS_TIMEOUT,
                            Map.of(PEER_NS, Set.of()),
                            Map.of(PEER_NS, Map.of()),
                            new ConsumerKey<>(PEER_NS, peerConsumersPods)));

                    sentRecords = sendRecords("producing records for active", null,
                            IntStream.range(0, partitions).boxed()
                                    .flatMap(partition -> topics.stream().map(topic ->
                                            record(topic, partition, msgCounter.incrementAndGet())))
                                    .toList());
                    expectedLatestOffset += sentRecords.size() / topics.size() / partitions;

                    Assertions.assertTrue(parallelConsume(POLL_TIMEOUT,
                            Map.of(PEER_NS, recordsAsStrings(sentRecords, pRecordsAsString)),
                            Map.of(PEER_NS, offsetsMap(topics, expectedLatestOffset)),
                            new ConsumerKey<>(PEER_NS, peerConsumersPods)));

                    // ----------------------------------------
                    // ----------------------------------------

                    log.info("""
                            BG-Operation:warmup #{}
                            """, i + 1);
                    states = states(String.format("2024-01-01T10:%02d:00Z", ++timeCounter), new StateVersion(State.CANDIDATE, ver3Candidate), new StateVersion(State.ACTIVE, ver3Active));

                    updatePublishersVersions(states, statePublisherOrigin, statePublisherPeer);

                    log.info("consume and commit all records by active, candidate consumers");

                    Assertions.assertTrue(parallelConsume(NO_RECORDS_TIMEOUT,
                            Map.of(ORIGIN_NS, Set.of(), PEER_NS, Set.of()),
                            Map.of(ORIGIN_NS, Map.of(), PEER_NS, Map.of()),
                            new ConsumerKey<>(ORIGIN_NS, originConsumersPods),
                            new ConsumerKey<>(PEER_NS, peerConsumersPods)));

                    List<ProducerRecord<String, String>> sentRecordsV3Active = sendRecords("producing records for active", ver3Active,
                            IntStream.range(0, partitions).boxed()
                                    .flatMap(partition -> topics.stream().map(topic ->
                                            record(topic, partition, msgCounter.incrementAndGet())))
                                    .toList());
                    expectedLatestOffset += sentRecordsV3Active.size() / topics.size() / partitions;

                    List<ProducerRecord<String, String>> sentRecordsV3Candidate = sendRecords("producing records for candidate", ver3Candidate,
                            IntStream.range(0, partitions).boxed()
                                    .flatMap(partition -> topics.stream().map(topic ->
                                            record(topic, partition, msgCounter.incrementAndGet())))
                                    .toList());
                    expectedLatestOffset += sentRecordsV3Candidate.size() / topics.size() / partitions;

                    Assertions.assertTrue(parallelConsume(POLL_TIMEOUT,
                            Map.of(ORIGIN_NS, recordsAsStrings(sentRecordsV3Candidate, pRecordsAsString), PEER_NS, recordsAsStrings(sentRecordsV3Active, pRecordsAsString)),
                            Map.of(ORIGIN_NS, offsetsMap(topics, expectedLatestOffset), PEER_NS, offsetsMap(topics, expectedLatestOffset)),
                            new ConsumerKey<>(ORIGIN_NS, originConsumersPods),
                            new ConsumerKey<>(PEER_NS, peerConsumersPods)));

                    // ----------------------------------------

                    log.info("""
                            BG-Operation:promote #{}
                            """, i + 2);
                    states = states(String.format("2024-01-01T10:%02d:00Z", ++timeCounter), new StateVersion(State.ACTIVE, ver4Active), new StateVersion(State.LEGACY, ver4Legacy));

                    updatePublishersVersions(states, statePublisherOrigin, statePublisherPeer);

                    log.info("consume and commit all records by active, legacy consumers");

                    Assertions.assertTrue(parallelConsume(NO_RECORDS_TIMEOUT,
                            Map.of(ORIGIN_NS, Set.of(), PEER_NS, Set.of()),
                            Map.of(ORIGIN_NS, Map.of(), PEER_NS, Map.of()),
                            new ConsumerKey<>(ORIGIN_NS, originConsumersPods),
                            new ConsumerKey<>(PEER_NS, peerConsumersPods)));

                    List<ProducerRecord<String, String>> sentRecordsV4Active = sendRecords("producing records for active", ver4Active,
                            IntStream.range(0, partitions).boxed()
                                    .flatMap(partition -> topics.stream().map(topic ->
                                            record(topic, partition, msgCounter.incrementAndGet())))
                                    .toList());
                    expectedLatestOffset += sentRecordsV4Active.size() / topics.size() / partitions;

                    List<ProducerRecord<String, String>> sentRecordsV4Legacy = sendRecords("producing records for legacy", ver4Legacy,
                            IntStream.range(0, partitions).boxed()
                                    .flatMap(partition -> topics.stream().map(topic ->
                                            record(topic, partition, msgCounter.incrementAndGet())))
                                    .toList());
                    expectedLatestOffset += sentRecordsV4Legacy.size() / topics.size() / partitions;

                    Assertions.assertTrue(parallelConsume(POLL_TIMEOUT,
                            Map.of(ORIGIN_NS, recordsAsStrings(sentRecordsV4Active, pRecordsAsString), PEER_NS, recordsAsStrings(sentRecordsV4Legacy, pRecordsAsString)),
                            Map.of(ORIGIN_NS, offsetsMap(topics, expectedLatestOffset), PEER_NS, offsetsMap(topics, expectedLatestOffset)),
                            new ConsumerKey<>(ORIGIN_NS, originConsumersPods),
                            new ConsumerKey<>(PEER_NS, peerConsumersPods)));

                    // ----------------------------------------

                    log.info("""
                            BG-Operation:commit #{}
                            """, i + 1);
                    states = states(String.format("2024-01-01T10:%02d:00Z", ++timeCounter), new StateVersion(State.ACTIVE, ver4Active), new StateVersion(State.IDLE, null));

                    updatePublishersVersions(states, statePublisherOrigin, statePublisherPeer);

                    log.info("consume and commit all records by active consumer");

                    Assertions.assertTrue(parallelConsume(NO_RECORDS_TIMEOUT,
                            Map.of(ORIGIN_NS, Set.of()),
                            Map.of(ORIGIN_NS, Map.of()),
                            new ConsumerKey<>(ORIGIN_NS, originConsumersPods)));

                    sentRecords = sendRecords("producing records for active", null,
                            IntStream.range(0, partitions).boxed()
                                    .flatMap(partition -> topics.stream().map(topic ->
                                            record(topic, partition, msgCounter.incrementAndGet())))
                                    .toList());
                    expectedLatestOffset += sentRecords.size() / topics.size() / partitions;

                    Assertions.assertTrue(parallelConsume(POLL_TIMEOUT,
                            Map.of(ORIGIN_NS, recordsAsStrings(sentRecords, pRecordsAsString)),
                            Map.of(ORIGIN_NS, offsetsMap(topics, expectedLatestOffset)),
                            new ConsumerKey<>(ORIGIN_NS, originConsumersPods)));
                }
            }
        }
    }
}
