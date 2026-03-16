package com.netcracker.cloud.context.propagation.core.supports.strategies;


import com.netcracker.cloud.context.propagation.core.Strategy;

import java.util.Optional;
import java.util.function.Supplier;

/**
 *  {@link RestEasyDefaultStrategy} is deprecated and will be removed in future releases, please use
 *  {@link ContextStorageStrategy} instead of this.
 *  This class is simple proxy for {@link ContextStorageStrategy}
 */
@Deprecated
public class RestEasyDefaultStrategy<T> implements Strategy<T> {
    private final ContextStorageStrategy<T> contextStorageStrategy;

    public RestEasyDefaultStrategy(Class<T> tClass, Supplier<T> defaultContextObject) {
        this.contextStorageStrategy = new ContextStorageStrategy<>(tClass, defaultContextObject);
    }

    @Override
    public void clear() {
        contextStorageStrategy.clear();
    }

    @Override
    public T get() {
        return contextStorageStrategy.get();
    }

    @Override
    public Optional<T> getSafe() {
        return contextStorageStrategy.getSafe();
    }

    @Override
    public void set(T value) {
        contextStorageStrategy.set(value);
    }
}
