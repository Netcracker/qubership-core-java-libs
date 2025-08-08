package com.netcracker.cloud.framework.contexts.xrequestid;

import org.qubership.cloud.context.propagation.core.Strategy;
import org.qubership.cloud.context.propagation.core.supports.strategies.DefaultStrategies;
import org.qubership.cloud.framework.contexts.strategies.AbstractXRequestIdStrategy;

import java.util.function.Supplier;

public class XRequestIdStrategy extends AbstractXRequestIdStrategy {

    private final Strategy<XRequestIdContextObject> strategy;

    public XRequestIdStrategy(Supplier<XRequestIdContextObject> defaultContextObject) {
        strategy = DefaultStrategies.threadLocalWithInheritanceDefaultStrategy(defaultContextObject);
    }

    @Override
    public Strategy<XRequestIdContextObject> getStrategy() {
        return strategy;
    }

}
