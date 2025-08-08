package org.qubership.cloud.framework.contexts.originatingbiid;

import org.qubership.cloud.context.propagation.core.Strategy;
import org.qubership.cloud.context.propagation.core.supports.strategies.DefaultStrategies;
import org.qubership.cloud.framework.contexts.strategies.AbstractOriginatingBiIdStrategy;

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
