package com.netcracker.cloud.context.propagation.core.supports.providers;

import org.qubership.cloud.context.propagation.core.Strategy;
import org.qubership.cloud.context.propagation.core.supports.strategies.DefaultStrategies;
import org.qubership.cloud.context.propagation.core.supports.strategies.ThreadLocalWithInheritanceDefaultStrategy;

public abstract class AbstractContextProviderOnInheritableThreadLocal<V> extends AbstractContextProvider<V> {

    private ThreadLocalWithInheritanceDefaultStrategy<V> threadLocal = DefaultStrategies.threadLocalWithInheritanceDefaultStrategy(defaultContextObject());

    @Override
    public Strategy<V> strategy() {
        return threadLocal;
    }
}
