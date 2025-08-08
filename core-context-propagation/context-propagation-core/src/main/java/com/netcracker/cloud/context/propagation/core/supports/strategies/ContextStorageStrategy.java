package com.netcracker.cloud.context.propagation.core.supports.strategies;

import org.qubership.cloud.context.propagation.core.Strategy;

import java.util.Optional;
import java.util.function.Supplier;

public class ContextStorageStrategy<T> implements Strategy<T> {
    private final Class<T> tClass;
    private Supplier<T> defaultContextObject;
    private ThreadLocal<Boolean> presents;

    public ContextStorageStrategy(Class<T> tClass, Supplier<T> defaultContextObject) {
        this.tClass = tClass;
        this.defaultContextObject = defaultContextObject;
        this.presents = ThreadLocal.withInitial(()-> false);
    }

    @Override
    public void clear() {
        ContextStorage.getContextDataMap(true).remove(tClass);
        this.presents.remove();
    }

    @Override
    public T get() {
        T contextData = (T) ContextStorage.getContextData(tClass);
        if (contextData != null || presents.get()) {
            return contextData;
        }
        T defaultValue = defaultContextObject.get();
        set(defaultValue);

        return defaultValue;
    }

    @Override
    public Optional<T> getSafe() {
        try {
            return Optional.ofNullable(get());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public void set(T value) {
        ContextStorage.pushContext(tClass, value);
        presents.set(true);
    }
}
