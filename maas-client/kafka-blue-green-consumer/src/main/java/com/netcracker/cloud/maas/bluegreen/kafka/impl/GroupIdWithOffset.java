package org.qubership.cloud.maas.bluegreen.kafka.impl;

import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

import java.util.Map;

public record GroupIdWithOffset(GroupId groupId, Map<TopicPartition, OffsetAndMetadata> offset) {
}
