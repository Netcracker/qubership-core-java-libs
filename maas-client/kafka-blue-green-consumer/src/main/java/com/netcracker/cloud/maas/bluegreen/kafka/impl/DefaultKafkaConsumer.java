package com.netcracker.cloud.maas.bluegreen.kafka.impl;

import com.netcracker.cloud.maas.bluegreen.kafka.BGKafkaConsumer;
import com.netcracker.cloud.maas.bluegreen.kafka.CommitMarker;
import com.netcracker.cloud.maas.bluegreen.kafka.Record;
import com.netcracker.cloud.maas.bluegreen.kafka.RecordsBatch;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;

import java.time.Duration;
import java.util.*;

/**
 * Useful as backward compatible layer to BGKafkaConsumer interface for non blue-green usages
 *
 * @param <K> record key type
 * @param <V> record value type
 */
@Slf4j
public class DefaultKafkaConsumer<K, V> implements BGKafkaConsumer<K, V> {

    private final Consumer<K, V> consumer;

    public DefaultKafkaConsumer(BGKafkaConsumerConfig config) {
        this.consumer = config.getConsumerSupplier().apply(config.getProperties());
        log.info("Subscribing to: {}", new TreeSet<>(config.getTopics()));
        this.consumer.subscribe(config.getTopics(), new ConsumerRebalanceListener() {
            @Override
            public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                log.debug("Partitions were revoked: {}", partitions);
            }

            @Override
            public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                log.debug("Partitions were assigned: {}", partitions);
                consumer.committed(new HashSet<>(partitions)).forEach(((topicPartition, offsetAndMetadata) ->
                        consumer.seek(topicPartition, Optional.ofNullable(offsetAndMetadata).map(OffsetAndMetadata::offset).orElse(0L))));
            }
        });
    }

    @Override
    public Optional<RecordsBatch<K, V>> poll(Duration timeout) {
        ConsumerRecords<K, V> records = consumer.poll(timeout);
        if (records.count() == 0) {
            return Optional.empty();
        }

        List<Record<K, V>> batch = new ArrayList<>(records.count());
        Map<TopicPartition, OffsetAndMetadata> position = new HashMap<>();
        CommitMarker marker = null;
        for (ConsumerRecord<K, V> record : records) {
            position.put(new TopicPartition(record.topic(), record.partition()), new OffsetAndMetadata(record.offset() + 1));
            marker = new CommitMarker(null, new HashMap<>(position));
            batch.add(new Record(record, marker));
        }
        return Optional.of(new RecordsBatch<>(batch, marker));
    }

    @Override
    public void commitSync(CommitMarker marker) {
        consumer.commitSync(marker.getPosition());
    }

    @Override
    public void pause() {
        this.consumer.pause(this.assignment());
    }

    @Override
    public void resume() {
        this.consumer.resume(this.assignment());
    }

    @Override
    public Set<TopicPartition> paused() {
        return this.consumer.paused();
    }

    public Collection<TopicPartition> assignment() {
        return this.consumer.assignment();
    }

    @Override
    public void close() {
        log.info("Close consumer");
        consumer.close();
    }
}
