package com.netcracker.cloud.framework.quarkus.contexts.originatingbiid;


import com.netcracker.cloud.context.propagation.core.RegisterProvider;
import com.netcracker.cloud.context.propagation.core.Strategy;
import com.netcracker.cloud.framework.contexts.originatingbiid.OriginatingBiIdContextObject;
import com.netcracker.cloud.framework.contexts.originatingbiid.OriginatingBiIdProvider;

@RegisterProvider
public class QuarkusOriginatingBiIdProvider extends OriginatingBiIdProvider {

    private final Strategy<OriginatingBiIdContextObject> originatingBiIdStrategy = new QuarkusOriginatingBiIdStrategyImpl(() -> provide(null));

    @Override
    public Strategy<OriginatingBiIdContextObject> strategy() {
        return originatingBiIdStrategy;
    }

    @Override
    public int providerOrder() {
        return -100;
    }
}
