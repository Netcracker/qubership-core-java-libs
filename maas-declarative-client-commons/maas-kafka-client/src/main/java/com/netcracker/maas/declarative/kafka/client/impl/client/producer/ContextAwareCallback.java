package com.netcracker.maas.declarative.kafka.client.impl.client.producer;

import com.netcracker.cloud.context.propagation.core.ContextManager;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.Map;

class ContextAwareCallback implements Callback {

    private final Callback callback;
    private final Map<String, Object> contextSnapshot;

    ContextAwareCallback(Callback callback) {
        this.callback = callback;
        this.contextSnapshot = ContextManager.createContextSnapshot();
    }

    @Override
    public void onCompletion(RecordMetadata recordMetadata, Exception e) {
        try {
            ContextManager.activateContextSnapshot(contextSnapshot);
            callback.onCompletion(recordMetadata, e);
        } finally {
            ContextManager.clearAll();
        }
    }
}
