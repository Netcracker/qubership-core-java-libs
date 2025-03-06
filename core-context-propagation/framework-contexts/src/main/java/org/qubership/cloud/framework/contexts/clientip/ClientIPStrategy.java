package org.qubership.cloud.framework.contexts.clientip;

import org.qubership.cloud.context.propagation.core.Strategy;
import org.qubership.cloud.context.propagation.core.supports.strategies.DefaultStrategies;
import org.qubership.cloud.framework.contexts.strategies.AbstractClientIPStrategy;

import java.util.function.Supplier;

public class ClientIPStrategy extends AbstractClientIPStrategy {
    private final Strategy<ClientIPContextObject> strategy;

    public ClientIPStrategy(Supplier<ClientIPContextObject> defaultContextObject) {
        strategy = DefaultStrategies.threadLocalWithInheritanceDefaultStrategy(defaultContextObject);
    }

    @Override
    public Strategy<ClientIPContextObject> getStrategy() {
        return strategy;
    }
}
