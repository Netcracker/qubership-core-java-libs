package com.netcracker.cloud.framework.quarkus.contexts.xrequestid;

import org.qubership.cloud.context.propagation.core.Strategy;
import org.qubership.cloud.context.propagation.core.supports.strategies.RestEasyDefaultStrategy;
import org.qubership.cloud.framework.contexts.strategies.AbstractXRequestIdStrategy;
import org.qubership.cloud.framework.contexts.xrequestid.XRequestIdContextObject;

import java.util.function.Supplier;

public class QuarkusXRequestStrategyImpl extends AbstractXRequestIdStrategy {
    private final Strategy<XRequestIdContextObject> strategy;

    public QuarkusXRequestStrategyImpl(Supplier<XRequestIdContextObject> defaultContextObject) {
        strategy = new RestEasyDefaultStrategy<>(XRequestIdContextObject.class, defaultContextObject);
    }

    @Override
    public Strategy<XRequestIdContextObject> getStrategy() {
        return strategy;
    }
}
