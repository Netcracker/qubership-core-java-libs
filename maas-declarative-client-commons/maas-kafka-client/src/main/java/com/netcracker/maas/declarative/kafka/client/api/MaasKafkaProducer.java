package com.netcracker.maas.declarative.kafka.client.api;

import com.netcracker.cloud.maas.client.api.kafka.TopicAddress;
import com.netcracker.maas.declarative.kafka.client.api.model.MaasProducerRecord;
import com.netcracker.maas.declarative.kafka.client.api.model.definition.MaasKafkaProducerDefinition;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Maas kafka producer interface
 */
public interface MaasKafkaProducer extends MaasKafkaClient {

    /**
     * Returns kafka topic, if {@link MaasKafkaProducerDefinition#isTenant()} returns true,
     * tenant in context will be used to get topic
     *
     * @return kafka topic
     */
    TopicAddress getTopic();

    /**
     * Asynchronously send a record to a topic and returns {@code CompletableFuture<RecordMetadata>}
     *
     * @param record maas kafka producer record
     * @param <K> key value type
     * @param <V> value value type
     * @return record meta data
     */
    <K, V> CompletableFuture<RecordMetadata> sendAsync(MaasProducerRecord<K, V> record);

    /**
     * Synchronously send a record to a topic and wait for broker ack using the configured send-sync timeout.
     *
     * @param record maas kafka producer record
     * @param <K> key value type
     * @param <V> value value type
     * @return recorded meta data
     */
    <K, V> RecordMetadata sendSync(MaasProducerRecord<K, V> record);

    /**
     * Synchronously send a record to a topic and wait for broker ack up to the given timeout.
     *
     * @param record  maas kafka producer record
     * @param timeout maximum time to wait for delivery ack after the record is submitted
     * @param <K>     key value type
     * @param <V>     value value type
     * @return recorded meta data
     */
    <K, V> RecordMetadata sendSync(MaasProducerRecord<K, V> record, Duration timeout);

    /**
     * Returns default kafka producer, if {@link MaasKafkaProducerDefinition#isTenant()}
     * returns true, tenant in context will be used to get producer
     *
     * @return default kafka producer
     */
    Producer unwrap();


    Producer unwrap(String tenantId);

    /**
     * Returns definition object that has been used to create this producer
     * @return MaasKafkaProducerDefinition
     */
    MaasKafkaProducerDefinition getDefinition();
}
