package com.netcracker.maas.declarative.kafka.client.api.context.propagation;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;

public interface ContextPropagationService {

    void propagateDataToContext(ConsumerRecord record);

    void populateDataToHeaders(ProducerRecord record);

    void clear();

}
