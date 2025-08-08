package org.qubership.cloud.framework.quarkus.contexts.originatingbiid;

import org.qubership.cloud.context.propagation.core.Strategy;
import org.qubership.cloud.context.propagation.core.supports.strategies.RestEasyDefaultStrategy;
import org.qubership.cloud.framework.contexts.originatingbiid.OriginatingBiIdContextObject;
import org.qubership.cloud.framework.contexts.strategies.AbstractOriginatingBiIdStrategy;

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
