package com.netcracker.maas.declarative.kafka.client.api.model;

import org.apache.kafka.common.header.Headers;

import java.util.Objects;
import java.util.StringJoiner;

public class MaasProducerRecord<K, V> {

    private final Integer partition;
    private final K key;
    private final V value;
    private final Long timestamp;
    private final Headers headers;

    public MaasProducerRecord(Integer partition, K key, V value, Long timestamp, Headers headers) {
        this.partition = partition;
        this.key = key;
        this.value = value;
        this.timestamp = timestamp;
        this.headers = headers;
    }

    public Integer getPartition() {
        return partition;
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public Headers getHeaders() {
        return headers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MaasProducerRecord<?, ?> that = (MaasProducerRecord<?, ?>) o;
        return Objects.equals(partition, that.partition) && Objects.equals(key, that.key) && Objects.equals(value, that.value) && Objects.equals(timestamp, that.timestamp) && Objects.equals(headers, that.headers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(partition, key, value, timestamp, headers);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", MaasProducerRecord.class.getSimpleName() + "[", "]")
                .add("partition=" + partition)
                .add("key=" + key)
                .add("value=" + value)
                .add("timestamp=" + timestamp)
                .add("headers=" + headers)
                .toString();
    }
}
