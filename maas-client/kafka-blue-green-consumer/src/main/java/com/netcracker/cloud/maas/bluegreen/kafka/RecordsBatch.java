package com.netcracker.cloud.maas.bluegreen.kafka;

import lombok.Value;

import java.util.List;

@Value
public class RecordsBatch<K, V> {
    List<Record<K, V>> batch;
    CommitMarker commitMarker;
}
