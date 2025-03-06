package org.qubership.cloud.context.propagation.core.supports.strategies;

import org.qubership.cloud.context.propagation.core.Strategy;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Supplier;

public abstract class AbstractStrategy<V> implements Strategy<V> {
    abstract protected Strategy<V> getStrategy();

    abstract protected Supplier<V> defaultObjectSupplier();

    @Override
    public void clear() {
        getStrategy().clear();
    }

    @Override
    public void set(V value) {
        getStrategy().set(value);
    }

    @Override
    public V get() {
        return getStrategy().get();
    }

    @Override
    public Optional<V> getSafe() {
        return getStrategy().getSafe()
                .filter(this::isValid);
    }

    public boolean isValid(@Nullable V value) {
        return true;
    }
}