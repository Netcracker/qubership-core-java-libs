package com.netcracker.cloud.framework.contexts.originatingbiid;

import com.netcracker.cloud.context.propagation.core.Strategy;
import com.netcracker.cloud.context.propagation.core.supports.strategies.DefaultStrategies;
import com.netcracker.cloud.framework.contexts.strategies.AbstractOriginatingBiIdStrategy;

import java.util.function.Supplier;

public class OriginatingBiIdStrategy extends AbstractOriginatingBiIdStrategy {

    private final Strategy<OriginatingBiIdContextObject> strategy;

    public OriginatingBiIdStrategy(Supplier<OriginatingBiIdContextObject> defaultContextObject) {
        strategy = DefaultStrategies.threadLocalWithInheritanceDefaultStrategy(defaultContextObject);
    }

    @Override
    public Strategy<OriginatingBiIdContextObject> getStrategy() {
        return strategy;
    }

}
