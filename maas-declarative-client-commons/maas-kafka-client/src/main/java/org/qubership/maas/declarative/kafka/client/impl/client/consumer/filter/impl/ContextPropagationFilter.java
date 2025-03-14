package org.qubership.maas.declarative.kafka.client.impl.client.consumer.filter.impl;

import org.qubership.cloud.maas.bluegreen.kafka.Record;
import org.qubership.maas.declarative.kafka.client.api.context.propagation.ContextPropagationService;
import org.qubership.maas.declarative.kafka.client.api.filter.ConsumerRecordFilter;
import org.qubership.maas.declarative.kafka.client.impl.client.consumer.filter.Chain;

public class ContextPropagationFilter implements ConsumerRecordFilter {

    public static final int CONTEXT_PROPAGATION_ORDER = 10;

    private final ContextPropagationService contextPropagationService;

    public ContextPropagationFilter(ContextPropagationService contextPropagationService) {
        this.contextPropagationService = contextPropagationService;
    }

    @Override
    public void doFilter(Record<?, ?> record, Chain<Record<?, ?>> next) {
        try {
            contextPropagationService.propagateDataToContext(record.getConsumerRecord());
            next.doFilter(record);
        } finally {
            contextPropagationService.clear();
        }
    }

    @Override
    public int order() {
        return CONTEXT_PROPAGATION_ORDER;
    }
}
