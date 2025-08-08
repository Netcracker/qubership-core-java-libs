package com.netcracker.cloud.context.propagation.core.executors;

import com.netcracker.cloud.context.propagation.core.ContextManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.Callable;


/**
 * Wraps a delegate Callable with logic for setting up a ContextSnapshot before invoking the delegate Callable
 * and when the delegate finishes work we clear context.
 */
public class ContextPropagationCallable<V> implements Callable<V> {

    private static final Logger log = LoggerFactory.getLogger(ContextPropagationCallable.class);
    private final Map<String, Object> contextSnapshot;
    private final Callable<V> delegate;


    public ContextPropagationCallable(Map<String, Object> contextSnapshot, Callable<V> delegate) {
        this.contextSnapshot = contextSnapshot;
        this.delegate = delegate;
    }


    @Override
    public V call() throws Exception {
        try {
            ContextManager.activateContextSnapshot(contextSnapshot);
            return delegate.call();
        } catch (Exception ex) {
            log.error("Got error during running task: ", ex);
            throw ex;
        } finally {
            ContextManager.clearAll();
        }
    }
}
