package com.netcracker.cloud.context.propagation.core.executors;

import org.qubership.cloud.context.propagation.core.ContextManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Wraps a delegate Supplier with logic for setting up a ContextSnapshot before invoking the delegate Supplier
 * and when the delegate finishes work we clear context.
 */
public class ContextPropagationSupplier<V> implements Supplier<V> {

    private static final Logger log = LoggerFactory.getLogger(ContextPropagationSupplier.class);
    private final Map<String, Object> contextSnapshot;
    private final Supplier<V> delegate;


    public ContextPropagationSupplier(Map<String, Object> contextSnapshot, Supplier<V> delegate) {
        this.contextSnapshot = contextSnapshot;
        this.delegate = delegate;
    }

    @Override
    public V get() {
        try {
            ContextManager.activateContextSnapshot(contextSnapshot);
            return delegate.get();
        } catch (Exception ex) {
            log.error("Got error during running task: ", ex);
            throw ex;
        } finally {
            ContextManager.clearAll();
        }
    }
}
