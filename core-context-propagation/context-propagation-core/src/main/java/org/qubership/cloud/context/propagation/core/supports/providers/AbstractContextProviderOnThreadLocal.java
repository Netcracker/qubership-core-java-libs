package org.qubership.cloud.context.propagation.core.supports.providers;

import org.qubership.cloud.context.propagation.core.Strategy;
import org.qubership.cloud.context.propagation.core.supports.strategies.DefaultStrategies;
import org.qubership.cloud.context.propagation.core.supports.strategies.ThreadLocalDefaultStrategy;

public abstract class AbstractContextProviderOnThreadLocal<V> extends AbstractContextProvider<V> {

    private ThreadLocalDefaultStrategy<V> threadLocal = DefaultStrategies.threadLocalDefaultStrategy(defaultContextObject());

    @Override
    public Strategy<V> strategy() {
        return threadLocal;
    }
}
