package com.netcracker.cloud.maas.bluegreen.kafka;

import org.qubership.cloud.bluegreen.api.model.State;
import org.qubership.cloud.bluegreen.impl.service.InMemoryBlueGreenStatePublisher;
import org.qubership.cloud.maas.bluegreen.kafka.impl.BGKafkaConsumerImpl;
import org.qubership.cloud.maas.bluegreen.kafka.impl.DefaultKafkaConsumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.qubership.cloud.maas.bluegreen.kafka.impl.BGKafkaConsumerConfig.builder;
import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.*;

@Slf4j
class BGKafkaPauseResumeConsumerTest extends AbstractKafkaClusterTest {

    @Test
    void testBGConsumerPauseResume() {
        Set<String> topics = new TreeSet<>(Set.of("topic-1", "topic-2"));

        admin.createTopics(topics.stream().map(topic -> new NewTopic(topic, partitions, replicationFactor)).toList()).values().values()
                .forEach(future -> {
                    try {
                        future.get();
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                });

        var connectionProperties = Map.<String, Object>of(
                BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                GROUP_ID_CONFIG, "bg-pause-resume-multi-topic-group",
                ENABLE_AUTO_COMMIT_CONFIG, "false",
                KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class
        );

        States states = states("2024-01-01T10:00:00Z", new StateVersion(State.ACTIVE, "v1"), new StateVersion(State.CANDIDATE, "v2"));

        var statePublisherOrigin = new InMemoryBlueGreenStatePublisher(states.origin());
        var statePublisherPeer = new InMemoryBlueGreenStatePublisher(states.peer());

        Collection<BGKafkaConsumerImpl<String, String>> originConsumersPods = IntStream.range(0, pods).boxed().map(i ->
                new BGKafkaConsumerImpl<String, String>(builder(connectionProperties, topics, statePublisherOrigin)
                        .deserializers(new StringDeserializer(), new StringDeserializer())
                        .consistencyMode(ConsumerConsistencyMode.GUARANTEE_CONSUMPTION)
                        .build())).toList();

        Collection<BGKafkaConsumerImpl<String, String>> peerConsumersPods = IntStream.range(0, pods).boxed().map(i ->
                new BGKafkaConsumerImpl<String, String>(builder(connectionProperties, topics, statePublisherPeer)
                        .deserializers(new StringDeserializer(), new StringDeserializer())
                        .consistencyMode(ConsumerConsistencyMode.GUARANTEE_CONSUMPTION)
                        .build())).toList();

        AtomicInteger msgCounter = new AtomicInteger(0);
        try (ConsumersCloseable originConsumersCloseable = new ConsumersCloseable(originConsumersPods);
             ConsumersCloseable peerConsumersCloseable = new ConsumersCloseable(peerConsumersPods)) {

            Assertions.assertTrue(parallelConsume(NO_RECORDS_TIMEOUT,
                    Map.of(ORIGIN_NS, Set.of(), PEER_NS, Set.of()),
                    Map.of(ORIGIN_NS, Map.of(), PEER_NS, Map.of()),
                    new ConsumerKey<>(ORIGIN_NS, originConsumersPods),
                    new ConsumerKey<>(PEER_NS, peerConsumersPods)));

            // send to active
            List<ProducerRecord<String, String>> sentRecordsActive = sendRecords("producing records for active", null,
                    IntStream.range(0, partitions).boxed().flatMap(partition -> topics.stream().map(topic ->
                            record(topic, partition, msgCounter.incrementAndGet()))).toList());
            long expectedLatestOffset = sentRecordsActive.size() / topics.size() / partitions;

            // send to candidate
            List<ProducerRecord<String, String>> sentRecordsCandidate = sendRecords("producing records for candidate", "v2",
                    IntStream.range(0, partitions).boxed().flatMap(partition -> topics.stream().map(topic ->
                            record(topic, partition, msgCounter.incrementAndGet()))).toList());
            expectedLatestOffset += sentRecordsCandidate.size() / topics.size() / partitions;

            // read all active messages
            // read all candidate messages

            Assertions.assertTrue(parallelConsume(POLL_TIMEOUT,
                    Map.of(ORIGIN_NS, recordsAsStrings(sentRecordsActive, pRecordsAsString), PEER_NS, recordsAsStrings(sentRecordsCandidate, pRecordsAsString)),
                    Map.of(ORIGIN_NS, offsetsMap(topics, expectedLatestOffset), PEER_NS, offsetsMap(topics, expectedLatestOffset)),
                    new ConsumerKey<>(ORIGIN_NS, originConsumersPods),
                    new ConsumerKey<>(PEER_NS, peerConsumersPods)));

            // pause all consumers from origin/peer
            Stream.concat(originConsumersPods.stream(), peerConsumersPods.stream()).forEach(consumer -> {
                consumer.pause();
                Assertions.assertFalse(consumer.paused().isEmpty());
            });

            // send active messages
            sentRecordsActive = sendRecords("producing records for active", null,
                    IntStream.range(0, partitions).boxed().flatMap(partition -> topics.stream().map(topic ->
                            record(topic, partition, msgCounter.incrementAndGet()))).toList());
            expectedLatestOffset += sentRecordsActive.size() / topics.size() / partitions;

            // send candidate messages
            sentRecordsCandidate = sendRecords("producing records for candidate", "v2",
                    IntStream.range(0, partitions).boxed().flatMap(partition -> topics.stream().map(topic ->
                            record(topic, partition, msgCounter.incrementAndGet()))).toList());
            expectedLatestOffset += sentRecordsCandidate.size() / topics.size() / partitions;

            // resume origin consumers one by one and read messages
            List<ProducerRecord<String, String>> recordsActive = sentRecordsActive;
            long offset = expectedLatestOffset;
            originConsumersPods.forEach(consumer -> {
                consumer.resume();
                Set<Integer> partitions = consumer.assignment().stream().map(TopicPartition::partition).collect(Collectors.toSet());
                List<ProducerRecord<String, String>> consumerRecords = recordsActive.stream().filter(r -> partitions.contains(r.partition())).toList();
                Assertions.assertTrue(parallelConsume(POLL_TIMEOUT,
                        Map.of(ORIGIN_NS, recordsAsStrings(consumerRecords, pRecordsAsString)),
                        Map.of(ORIGIN_NS, offsetsMap(topics, offset).entrySet().stream()
                                .filter(entry -> partitions.contains(entry.getKey().partition()))
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))),
                        new ConsumerKey<>(ORIGIN_NS, List.of(consumer))));
            });

            // resume peer consumers one by one and read messages
            List<ProducerRecord<String, String>> recordsCandidate = sentRecordsCandidate;
            peerConsumersPods.forEach(consumer -> {
                consumer.resume();
                Set<Integer> partitions = consumer.assignment().stream().map(TopicPartition::partition).collect(Collectors.toSet());
                List<ProducerRecord<String, String>> consumerRecords = recordsCandidate.stream().filter(r -> partitions.contains(r.partition())).toList();
                Assertions.assertTrue(parallelConsume(POLL_TIMEOUT,
                        Map.of(PEER_NS, recordsAsStrings(consumerRecords, pRecordsAsString)),
                        Map.of(PEER_NS, offsetsMap(topics, offset).entrySet().stream()
                                .filter(entry -> partitions.contains(entry.getKey().partition()))
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))),
                        new ConsumerKey<>(PEER_NS, List.of(consumer))));
            });
        }
    }

    @Test
    void testDefaultConsumerPauseResume() {
        Set<String> topics = new TreeSet<>(Set.of("topic-1", "topic-2"));

        admin.createTopics(topics.stream().map(topic -> new NewTopic(topic, partitions, replicationFactor)).toList()).values().values()
                .forEach(future -> {
                    try {
                        future.get();
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                });

        var connectionProperties = Map.<String, Object>of(
                BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                GROUP_ID_CONFIG, "default-pause-resume-multi-topic-group",
                ENABLE_AUTO_COMMIT_CONFIG, "false",
                KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class
        );

        States states = states("2024-01-01T10:00:00Z", new StateVersion(State.ACTIVE, "v1"));
        var statePublisher = new InMemoryBlueGreenStatePublisher(states.origin());

        Collection<DefaultKafkaConsumer<String, String>> consumersPods = IntStream.range(0, pods).boxed().parallel().map(i ->
                new DefaultKafkaConsumer<String, String>(builder(connectionProperties, topics, statePublisher)
                        .deserializers(new StringDeserializer(), new StringDeserializer())
                        .build())).toList();

        AtomicInteger msgCounter = new AtomicInteger(0);
        try (ConsumersCloseable consumersCloseable = new ConsumersCloseable(consumersPods)) {

            Assertions.assertTrue(parallelConsume(NO_RECORDS_TIMEOUT,
                    Map.of(ORIGIN_NS, Set.of()),
                    Map.of(ORIGIN_NS, Map.of()),
                    new ConsumerKey<>(ORIGIN_NS, consumersPods)));

            // send messages
            List<ProducerRecord<String, String>> sentRecords = sendRecords("producing records before pause", null,
                    IntStream.range(0, partitions).boxed().flatMap(partition -> topics.stream().map(topic ->
                            record(topic, partition, msgCounter.incrementAndGet()))).toList());
            long expectedLatestOffset = sentRecords.size() / topics.size() / partitions;

            // read all messages
            Assertions.assertTrue(parallelConsume(POLL_TIMEOUT,
                    Map.of(ORIGIN_NS, recordsAsStrings(sentRecords, pRecordsAsString)),
                    Map.of(ORIGIN_NS, offsetsMap(topics, expectedLatestOffset)),
                    new ConsumerKey<>(ORIGIN_NS, consumersPods)));

            // pause all consumers
            consumersPods.forEach(consumer -> {
                consumer.pause();
                Assertions.assertFalse(consumer.paused().isEmpty());
            });

            // send messages
            sentRecords = sendRecords("producing records after pause", null,
                    IntStream.range(0, partitions).boxed().flatMap(partition -> topics.stream().map(topic ->
                            record(topic, partition, msgCounter.incrementAndGet()))).toList());
            expectedLatestOffset += sentRecords.size() / topics.size() / partitions;

            // resume consumers one by one and read messages
            List<ProducerRecord<String, String>> recordsActive = sentRecords;
            long offset = expectedLatestOffset;
            consumersPods.forEach(consumer -> {
                consumer.resume();
                Set<Integer> partitions = consumer.assignment().stream().map(TopicPartition::partition).collect(Collectors.toSet());
                List<ProducerRecord<String, String>> consumerRecords = recordsActive.stream().filter(r -> partitions.contains(r.partition())).toList();
                Assertions.assertTrue(parallelConsume(POLL_TIMEOUT,
                        Map.of(ORIGIN_NS, recordsAsStrings(consumerRecords, pRecordsAsString)),
                        Map.of(ORIGIN_NS, offsetsMap(topics, offset).entrySet().stream()
                                .filter(entry -> partitions.contains(entry.getKey().partition()))
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))),
                        new ConsumerKey<>(ORIGIN_NS, List.of(consumer))));
            });
        }
    }

    @Test
    void testBGConsumerPauseResumeNotInitiated() {
        Set<String> topics = new TreeSet<>(Set.of("topic-1", "topic-2"));
        var connectionProperties = Map.<String, Object>of(
                BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                GROUP_ID_CONFIG, "bg-pause-resume-multi-topic-group",
                ENABLE_AUTO_COMMIT_CONFIG, "false",
                KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class
        );
        States states = states("2024-01-01T10:00:00Z", new StateVersion(State.ACTIVE, "v1"), new StateVersion(State.CANDIDATE, "v2"));

        var statePublisherOrigin = new InMemoryBlueGreenStatePublisher(states.origin());
        BGKafkaConsumerImpl<String, String> bgKafkaConsumer = new BGKafkaConsumerImpl<>(builder(connectionProperties, topics, statePublisherOrigin)
                .deserializers(new StringDeserializer(), new StringDeserializer())
                .consistencyMode(ConsumerConsistencyMode.GUARANTEE_CONSUMPTION)
                .build());
        Assertions.assertThrows(IllegalStateException.class, bgKafkaConsumer::assignment, "Consumer not initiated yet. Initiate with poll()");
        Assertions.assertThrows(IllegalStateException.class, bgKafkaConsumer::paused, "Consumer not initiated yet. Initiate with poll()");
        Assertions.assertThrows(IllegalStateException.class, bgKafkaConsumer::pause, "Consumer not initiated yet. Initiate with poll()");
        Assertions.assertThrows(IllegalStateException.class, bgKafkaConsumer::resume, "Consumer not initiated yet. Initiate with poll()");
    }
}
