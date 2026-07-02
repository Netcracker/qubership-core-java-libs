package com.netcracker.cloud.maas.bluegreen.kafka;

import org.apache.kafka.common.TopicPartition;

import java.util.Collection;

@FunctionalInterface
public interface PartitionsAssignedListener {
    void onPartitionsAssigned(Collection<TopicPartition> partitions);
}
