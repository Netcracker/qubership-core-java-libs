package com.netcracker.maas.declarative.kafka.client.api;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.List;

/**
 * Interface described error handler for maas kafka consumer
 */
public interface MaasKafkaConsumerErrorHandler {

    /**
     * Method invoked when exception occurred to handle it
     *
     * @param exception      usual error
     * @param errorRecord    usual kafka consumer record or null
     * @param handledRecords successfully handled records at on pool call
     * @throws Exception when something bad happens
     */
    void handle(Exception exception, ConsumerRecord<?, ?> errorRecord, List<ConsumerRecord<?, ?>> handledRecords) throws Exception;
}
