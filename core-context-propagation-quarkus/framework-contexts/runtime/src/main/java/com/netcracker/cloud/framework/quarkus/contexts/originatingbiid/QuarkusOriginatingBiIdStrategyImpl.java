package com.netcracker.cloud.framework.quarkus.contexts.originatingbiid;

import com.netcracker.cloud.context.propagation.core.Strategy;
import com.netcracker.cloud.context.propagation.core.supports.strategies.RestEasyDefaultStrategy;
import com.netcracker.cloud.framework.contexts.originatingbiid.OriginatingBiIdContextObject;
import com.netcracker.cloud.framework.contexts.strategies.AbstractOriginatingBiIdStrategy;

import java.util.function.Supplier;

public class QuarkusOriginatingBiIdStrategyImpl extends AbstractOriginatingBiIdStrategy {
    private final Strategy<OriginatingBiIdContextObject> strategy;

    public QuarkusOriginatingBiIdStrategyImpl(Supplier<OriginatingBiIdContextObject> defaultContextObject) {
        strategy = new RestEasyDefaultStrategy<>(OriginatingBiIdContextObject.class, defaultContextObject);
    }

    @Override
    public Strategy<OriginatingBiIdContextObject> getStrategy() {
        return strategy;
    }
}
