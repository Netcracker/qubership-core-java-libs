package com.netcracker.maas.declarative.kafka.client.impl.common.context.propagation;

import com.netcracker.cloud.context.propagation.core.RequestContextPropagation;
import com.netcracker.cloud.maas.client.context.kafka.KafkaContextPropagation;
import com.netcracker.maas.declarative.kafka.client.api.context.propagation.ContextPropagationService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;

public class DefaultContextPropagationServiceImpl implements ContextPropagationService {

    @Override
    public void propagateDataToContext(ConsumerRecord consumerRecord) {
        KafkaContextPropagation.restoreContext(consumerRecord.headers());
    }

    @Override
    public void populateDataToHeaders(ProducerRecord producerRecord) {
        KafkaContextPropagation.propagateContext().forEach(header -> producerRecord.headers().add(header));
    }

    @Override
    public void clear() {
        RequestContextPropagation.clear();
    }
}
