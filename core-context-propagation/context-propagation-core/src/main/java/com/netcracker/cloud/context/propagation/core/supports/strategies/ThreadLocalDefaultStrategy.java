package com.netcracker.cloud.context.propagation.core.supports.strategies;

import org.qubership.cloud.context.propagation.core.Strategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.function.Supplier;

public class ThreadLocalDefaultStrategy<V> implements Strategy<V> {

    private static final Logger log = LoggerFactory.getLogger(ThreadLocalDefaultStrategy.class);
    protected ThreadLocal<V> threadLocal;

    public ThreadLocalDefaultStrategy() {
        threadLocal = new ThreadLocal<>();
    }

    public ThreadLocalDefaultStrategy(Supplier<V> defaultContextObject) {
        threadLocal = ThreadLocal.withInitial(defaultContextObject);
    }

    @Override
    public void clear() {
        threadLocal.remove();
    }

    @Override
    public void set(V value) {
        threadLocal.set(value);
    }

    @Override
    public V get() {
        return threadLocal.get();
    }

    @Override
    public Optional<V> getSafe() {
        try {
            return Optional.ofNullable(get());
        } catch (Exception ex) {
            log.error("Got error while get context object: ", ex);
            return Optional.empty();
        }
    }
}
