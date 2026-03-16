package com.netcracker.cloud.context.propagation.core.supports.providers;

import com.netcracker.cloud.context.propagation.core.Strategy;
import com.netcracker.cloud.context.propagation.core.supports.strategies.DefaultStrategies;
import com.netcracker.cloud.context.propagation.core.supports.strategies.ThreadLocalDefaultStrategy;

public abstract class AbstractContextProviderOnThreadLocal<V> extends AbstractContextProvider<V> {

    private ThreadLocalDefaultStrategy<V> threadLocal = DefaultStrategies.threadLocalDefaultStrategy(defaultContextObject());

    @Override
    public Strategy<V> strategy() {
        return threadLocal;
    }
}
