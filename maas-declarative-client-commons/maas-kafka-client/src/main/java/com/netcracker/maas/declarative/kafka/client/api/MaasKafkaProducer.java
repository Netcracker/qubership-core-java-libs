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
     * Asynchronously send a record to a topic and returns {@code CompletableFuture<RecordMetadata>}.
     * This method does not apply a wait timeout. If Kafka never calls back, the future stays incomplete
     * until the caller stops waiting ({@code orTimeout} or {@code get(timeout, unit)}) or until Kafka's
     * {@code delivery.timeout.ms} fails the produce. Use {@link #sendSync(MaasProducerRecord)} when this
     * thread must block for the outcome.
     *
     * @param record maas kafka producer record
     * @param <K> key value type
     * @param <V> value value type
     * @return record meta data
     */
    <K, V> CompletableFuture<RecordMetadata> sendAsync(MaasProducerRecord<K, V> record);

    /**
     * Synchronously send a record to a topic and wait for broker ack using the configured send-sync timeout.
     * Failures are thrown as {@code MaasKafkaException}. {@code getCause()} is the Kafka exception,
     * {@code TimeoutException}, or {@code InterruptedException} directly, not an {@code ExecutionException}.
     * An interrupt restores the interrupt flag before the exception is thrown.
     *
     * @param record maas kafka producer record
     * @param <K> key value type
     * @param <V> value value type
     * @return recorded meta data
     */
    <K, V> RecordMetadata sendSync(MaasProducerRecord<K, V> record);

    /**
     * Synchronously send a record to a topic and wait for broker ack up to the given timeout.
     * The wait is {@code timeout.toMillis()} with no configured minimum other than 1 ms.
     * A positive duration that truncates to 0 ms is rejected.
     * Failures are thrown as {@code MaasKafkaException}. {@code getCause()} is the Kafka exception,
     * {@code TimeoutException}, or {@code InterruptedException} directly, not an {@code ExecutionException}.
     * An interrupt restores the interrupt flag before the exception is thrown.
     *
     * @param record  maas kafka producer record
     * @param timeout maximum time to wait for delivery ack after the record is submitted; at least 1 ms
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
