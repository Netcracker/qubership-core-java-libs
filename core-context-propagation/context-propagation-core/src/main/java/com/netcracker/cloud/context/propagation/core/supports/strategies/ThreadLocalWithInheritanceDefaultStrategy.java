package org.qubership.cloud.context.propagation.core.supports.strategies;

import java.util.function.Supplier;

public class ThreadLocalWithInheritanceDefaultStrategy<V> extends ThreadLocalDefaultStrategy<V> {

    public ThreadLocalWithInheritanceDefaultStrategy() {
        threadLocal = new InheritableThreadLocal<>();
    }

    public ThreadLocalWithInheritanceDefaultStrategy(Supplier<V> defaultContextObject) {
        super(defaultContextObject);
        threadLocal = new InheritableThreadLocal<V>() {
            @Override
            protected V initialValue() {
                return defaultContextObject.get();
            }
        };
    }
}
