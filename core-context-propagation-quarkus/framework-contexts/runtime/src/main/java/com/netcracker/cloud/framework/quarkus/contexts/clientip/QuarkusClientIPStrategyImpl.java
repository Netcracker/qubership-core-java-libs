package com.netcracker.cloud.framework.quarkus.contexts.clientip;

import com.netcracker.cloud.context.propagation.core.Strategy;
import com.netcracker.cloud.context.propagation.core.supports.strategies.RestEasyDefaultStrategy;
import com.netcracker.cloud.framework.contexts.clientip.ClientIPContextObject;
import com.netcracker.cloud.framework.contexts.strategies.AbstractClientIPStrategy;

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
