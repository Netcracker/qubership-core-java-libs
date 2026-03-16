package com.netcracker.cloud.maas.bluegreen.kafka;

import com.netcracker.cloud.bluegreen.api.service.BlueGreenStatePublisher;
import com.netcracker.cloud.maas.bluegreen.kafka.impl.*;
import com.netcracker.cloud.maas.bluegreen.kafka.impl.*;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.consumer.OffsetAndTimestamp;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.Mockito;
import com.netcracker.cloud.maas.bluegreen.kafka.impl.*;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG;

public class TestUtil {

    public static final String TEST_TOPIC_NAME = "test-topic";
    public static final String TEST_GROUP_NAME = "test";

    public static Stream<Arguments> arguments(String current, AdminAdapter adminAdapter) {
        return Stream.of(Arguments.of(GroupId.parse(current), config(adminAdapter), adminAdapter, indexer(adminAdapter), consumer()));
    }

    public static Stream<Arguments> arguments(String current, AdminAdapter adminAdapter, Consumer consumer) {
        return Stream.of(Arguments.of(GroupId.parse(current), config(adminAdapter), adminAdapter, indexer(adminAdapter), consumer));
    }

    public static Stream<Arguments> arguments(String current, AdminAdapter adminAdapter, BGKafkaConsumerConfig config, Consumer consumer) {
        return Stream.of(Arguments.of(GroupId.parse(current), config, adminAdapter, indexer(adminAdapter), consumer));
    }

    public static BGKafkaConsumerConfig config(AdminAdapter adminAdapter) {
        Map<String, Object> properties = Map.of(GROUP_ID_CONFIG, TEST_GROUP_NAME);
        Supplier<String> m2mTokenSupplier = () -> "fake";
        BlueGreenStatePublisher publisher = Mockito.mock(BlueGreenStatePublisher.class);
        BGKafkaConsumerConfig config = Mockito.spy(BGKafkaConsumerConfig.builder(properties, TEST_TOPIC_NAME, m2mTokenSupplier, publisher).build());
        Mockito.when(config.getAdminAdapterSupplier()).thenReturn(props -> adminAdapter);
//        Consumer consumer = Mockito.mock(Consumer.class);
//        Mockito.when(config.getConsumerSupplier()).thenReturn(props -> consumer);
        return config;
    }

    public static Consumer consumer(Map.Entry<ConsumerGroupListing, Map<TopicPartition, OffsetAndMetadata>>... groups) {
        Consumer consumer = Mockito.mock(Consumer.class);
        Mockito.when(consumer.partitionsFor(TEST_TOPIC_NAME))
                .thenReturn(IntStream.range(0, 2).boxed()
                        .map(p -> new PartitionInfo(TEST_TOPIC_NAME, p, null, null, null)).toList());

        Map<TopicPartition, OffsetAndMetadata> committed = new HashMap<>();
        IntStream.range(0, 2).boxed().forEach(p -> committed.put(new TopicPartition(TEST_TOPIC_NAME, p), null));
        Mockito.when(consumer.committed(Mockito.anySet())).thenReturn(committed);

        for (Map.Entry<ConsumerGroupListing, Map<TopicPartition, OffsetAndMetadata>> groupEntry : groups) {
            Mockito.when(consumer.committed(groupEntry.getValue().keySet())).thenReturn(groupEntry.getValue());
        }
        return consumer;
    }

    public static AdminAdapter adminAdapter(Map.Entry<ConsumerGroupListing, Map<TopicPartition, OffsetAndMetadata>>... groups) {
        AdminAdapter adminAdapter = Mockito.mock(AdminAdapter.class);
        mockAdminAdapter(adminAdapter, groups);
        return adminAdapter;
    }

    public static void mockAdminAdapter(AdminAdapter adminAdapter, Map.Entry<ConsumerGroupListing, Map<TopicPartition, OffsetAndMetadata>>... groups) {
        Map<ConsumerGroupListing, Map<TopicPartition, OffsetAndMetadata>> groupsMap =
                Arrays.stream(groups).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        Mockito.when(adminAdapter.listConsumerGroup()).thenReturn(groupsMap.keySet());

        groupsMap.forEach((group, topicOffsetMap) ->
                Mockito.when(adminAdapter.listConsumerGroupOffsets(Mockito.eq(group.groupId()))).thenReturn(topicOffsetMap));
    }

    public static Map.Entry<ConsumerGroupListing, Map<TopicPartition, OffsetAndMetadata>> group(String group, Map.Entry<TopicPartition, OffsetAndMetadata>... offsets) {
        return new AbstractMap.SimpleEntry<>(new ConsumerGroupListing(group, true), topicOffsetMap(offsets));
    }

    public static GroupIdWithOffset versionedGroupIdWithOffset(String versionedGroupId,
                                                               Map.Entry<TopicPartition, OffsetAndMetadata>... topicOffset) {
        return new GroupIdWithOffset(GroupId.parse(versionedGroupId), topicOffsetMap(topicOffset));
    }

    public static Map<TopicPartition, OffsetAndMetadata> topicOffsetMap(Map.Entry<TopicPartition, OffsetAndMetadata>... topicOffset) {
        return Arrays.stream(topicOffset).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static Map<TopicPartition, OffsetAndTimestamp> topicOffsetTimestampMap(Map.Entry<TopicPartition, OffsetAndTimestamp>... topicOffset) {
        return Arrays.stream(topicOffset).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static Map<TopicPartition, Long> topicOffsetLongMap(Map.Entry<TopicPartition, Long>... topicOffset) {
        return Arrays.stream(topicOffset).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static Map.Entry<TopicPartition, OffsetAndMetadata> topicOffset(int partition, long offset) {
        return new AbstractMap.SimpleEntry<>(topicPartition(partition), new OffsetAndMetadata(offset));
    }

    public static Map.Entry<TopicPartition, OffsetAndMetadata> topicOffset(String topic, int partition, long offset) {
        return new AbstractMap.SimpleEntry<>(topicPartition(topic, partition), new OffsetAndMetadata(offset));
    }

    public static Map.Entry<TopicPartition, OffsetAndTimestamp> topicOffsetTimestamp(int partition, long offset, long timestamp) {
        return new AbstractMap.SimpleEntry<>(topicPartition(partition), new OffsetAndTimestamp(offset, timestamp));
    }

    public static Map.Entry<TopicPartition, Long> topicOffsetLong(int partition, long offset) {
        return new AbstractMap.SimpleEntry<>(topicPartition(partition), offset);
    }

    public static TopicPartition topicPartition(int partition) {
        return topicPartition(TestUtil.TEST_TOPIC_NAME, partition);
    }

    public static TopicPartition topicPartition(String topic, int partition) {
        return new TopicPartition(topic, partition);
    }

    public static OffsetsIndexer indexer(AdminAdapter admin) {
        return new OffsetsIndexer(TEST_GROUP_NAME, admin);
    }

    public static PartitionInfo partitionInfo(int partition) {
        return new PartitionInfo(TEST_TOPIC_NAME, partition, null, null, null);
    }
}
