package com.netcracker.cloud.maas.bluegreen.kafka;

import lombok.Value;
import org.apache.kafka.clients.consumer.ConsumerRecord;

@Value
public class Record<K, V> {
    ConsumerRecord<K, V> consumerRecord;
    CommitMarker commitMarker;
}
