package com.netcracker.cloud.context.propagation.core.supports.providers;

import com.netcracker.cloud.context.propagation.core.ContextProvider;
import com.netcracker.cloud.context.propagation.core.Strategy;
import com.netcracker.cloud.context.propagation.core.contextdata.IncomingContextData;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public abstract class AbstractContextProvider<V> implements ContextProvider<V> {
    @Override
    public abstract Strategy<V> strategy();

    public abstract String contextName();

    @Override
    public abstract V provide(@Nullable IncomingContextData incomingContextData);

    protected Supplier<V> defaultContextObject() {
        return () -> provide(null);
    }

    @Override
    public int initLevel() {
        return 0;
    }

    @Override
    public int providerOrder() {
        return 0;
    }
}
