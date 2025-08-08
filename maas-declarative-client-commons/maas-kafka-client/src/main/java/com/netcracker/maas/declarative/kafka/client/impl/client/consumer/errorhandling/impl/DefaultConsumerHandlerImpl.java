package com.netcracker.maas.declarative.kafka.client.impl.client.consumer.errorhandling.impl;

import org.qubership.maas.declarative.kafka.client.api.MaasKafkaConsumerErrorHandler;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.errors.SerializationException;

import java.util.List;

public class DefaultConsumerHandlerImpl implements MaasKafkaConsumerErrorHandler {

    @Override
    public void handle(
            Exception exception,
            ConsumerRecord<?, ?> errorRecord,
            List<ConsumerRecord<?, ?>> handledRecords
    ) throws Exception {
        if (errorRecord == null && exception instanceof SerializationException) {
            throw new IllegalStateException("SerializationException cannot be processed by this handler");
        }

        // add custom exception handler
        throw exception;
    }
}
