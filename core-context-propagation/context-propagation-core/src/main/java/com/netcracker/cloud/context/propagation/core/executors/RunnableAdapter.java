package com.netcracker.cloud.context.propagation.core.executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

final class RunnableAdapter implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(RunnableAdapter.class);
    private final Callable<?> callable;

    RunnableAdapter(Callable<?> callable) {
        if (callable == null) throw new IllegalArgumentException("Callable to convert into runnable was <null>.");
        this.callable = callable;
    }

    public void run() {
        try {
            Object result = callable.call();
            log.debug("Call result ignored by RunnableAdapter: {}", result);
        } catch (RuntimeException unchecked) {
            throw unchecked;
        } catch (Exception checked) {
            throw new IllegalStateException("Checked exception thrown from call: " + checked.getMessage(), checked);
        }
    }



    @Override
    public String toString() {
        return getClass().getSimpleName() + '{' + callable + '}';
    }

}
