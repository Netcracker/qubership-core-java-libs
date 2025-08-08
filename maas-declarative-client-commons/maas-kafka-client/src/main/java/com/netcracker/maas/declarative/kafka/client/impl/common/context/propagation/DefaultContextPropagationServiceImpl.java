package com.netcracker.maas.declarative.kafka.client.impl.common.context.propagation;

import org.qubership.cloud.context.propagation.core.RequestContextPropagation;
import org.qubership.cloud.maas.client.context.kafka.KafkaContextPropagation;
import org.qubership.maas.declarative.kafka.client.api.context.propagation.ContextPropagationService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;

public class DefaultContextPropagationServiceImpl implements ContextPropagationService {

    @Override
    public void propagateDataToContext(ConsumerRecord record) {
        KafkaContextPropagation.restoreContext(record.headers());
    }

    @Override
    public void populateDataToHeaders(ProducerRecord record) {
        KafkaContextPropagation.propagateContext().forEach(header -> record.headers().add(header));
    }

    @Override
    public void clear() {
        RequestContextPropagation.clear();
    }
}
