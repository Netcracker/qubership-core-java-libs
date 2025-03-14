package org.qubership.maas.declarative.kafka.quarkus.client.impl;

import org.qubership.maas.declarative.kafka.client.impl.common.context.propagation.DefaultContextPropagationServiceImpl;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import org.apache.kafka.clients.consumer.ConsumerRecord;

public class QuarkusContextPropagationServiceImpl extends DefaultContextPropagationServiceImpl {

    @Override
    public void propagateDataToContext(ConsumerRecord consumerRecord) {
        ManagedContext requestContext = Arc.container().requestContext();
        if (!requestContext.isActive()) {
            requestContext.activate();
        }
        super.propagateDataToContext(consumerRecord);
    }

    @Override
    public void clear() {
        try {
            super.clear();
        } finally {
            ManagedContext requestContext = Arc.container().requestContext();
            requestContext.terminate();
        }
    }
}
