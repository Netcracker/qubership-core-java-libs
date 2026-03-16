package com.netcracker.cloud.maas.bluegreen.kafka;

import com.netcracker.cloud.bluegreen.api.model.NamespaceVersion;
import lombok.ToString;
import lombok.Value;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

import java.util.Map;

@Value
@ToString
public class CommitMarker {
    NamespaceVersion version;
    Map<TopicPartition, OffsetAndMetadata> position;
}
