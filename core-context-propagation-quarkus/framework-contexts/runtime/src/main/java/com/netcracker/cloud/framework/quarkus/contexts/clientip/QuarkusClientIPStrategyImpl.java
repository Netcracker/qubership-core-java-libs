package com.netcracker.cloud.framework.quarkus.contexts.clientip;

import org.qubership.cloud.context.propagation.core.Strategy;
import org.qubership.cloud.context.propagation.core.supports.strategies.RestEasyDefaultStrategy;
import org.qubership.cloud.framework.contexts.clientip.ClientIPContextObject;
import org.qubership.cloud.framework.contexts.strategies.AbstractClientIPStrategy;

import java.util.function.Supplier;

public class QuarkusClientIPStrategyImpl extends AbstractClientIPStrategy {
    private final Strategy<ClientIPContextObject> strategy;

    public QuarkusClientIPStrategyImpl(Supplier<ClientIPContextObject> defaultContextObject) {
        strategy = new RestEasyDefaultStrategy<>(ClientIPContextObject.class, defaultContextObject);
    }

    @Override
    public Strategy<ClientIPContextObject> getStrategy() {
        return strategy;
    }
}
