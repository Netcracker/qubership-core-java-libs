package com.netcracker.cloud.maas.bluegreen.kafka.impl;

import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

import java.util.Collection;
import java.util.Map;

public interface AdminAdapter extends AutoCloseable {
    Collection<ConsumerGroupListing> listConsumerGroup();

    Map<TopicPartition, OffsetAndMetadata> listConsumerGroupOffsets(String groupId);

    void alterConsumerGroupOffsets(GroupId groupIdPrefix, Map<TopicPartition, OffsetAndMetadata> proposedOffsets);

    void deleteConsumerGroupOffsets(GroupId grouIdPrefix, Map<TopicPartition, OffsetAndMetadata> proposedOffsets);

    void deleteConsumerGroups(Collection<String> groupIds);

    void close();
}
