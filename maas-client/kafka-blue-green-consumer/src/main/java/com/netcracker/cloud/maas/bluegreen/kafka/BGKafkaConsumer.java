package com.netcracker.cloud.maas.bluegreen.kafka;

import org.apache.kafka.common.TopicPartition;

import java.time.Duration;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public interface BGKafkaConsumer<K, V> extends AutoCloseable {
    Optional<RecordsBatch<K, V>> poll(Duration timeout);

    void commitSync(CommitMarker marker);

    void pause();

    void resume();

    Set<TopicPartition> paused();

    Collection<TopicPartition> assignment();

    void close();
}
