package com.netcracker.cloud.context.propagation.core.supports.providers;

import com.netcracker.cloud.context.propagation.core.Strategy;
import com.netcracker.cloud.context.propagation.core.supports.strategies.DefaultStrategies;
import com.netcracker.cloud.context.propagation.core.supports.strategies.ThreadLocalWithInheritanceDefaultStrategy;

public abstract class AbstractContextProviderOnInheritableThreadLocal<V> extends AbstractContextProvider<V> {

    private ThreadLocalWithInheritanceDefaultStrategy<V> threadLocal = DefaultStrategies.threadLocalWithInheritanceDefaultStrategy(defaultContextObject());

    @Override
    public Strategy<V> strategy() {
        return threadLocal;
    }
}
