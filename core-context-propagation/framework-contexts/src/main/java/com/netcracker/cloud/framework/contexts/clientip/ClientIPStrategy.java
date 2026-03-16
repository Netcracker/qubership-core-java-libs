package com.netcracker.cloud.framework.contexts.clientip;

import com.netcracker.cloud.context.propagation.core.Strategy;
import com.netcracker.cloud.context.propagation.core.supports.strategies.DefaultStrategies;
import com.netcracker.cloud.framework.contexts.strategies.AbstractClientIPStrategy;

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
