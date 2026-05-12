package com.netcracker.cloud.framework.quarkus.contexts.xchannelrequestid;

import com.netcracker.cloud.context.propagation.core.Strategy;
import com.netcracker.cloud.context.propagation.core.supports.strategies.RestEasyDefaultStrategy;
import com.netcracker.cloud.framework.contexts.strategies.AbstractXChannelRequestIdStrategy;
import com.netcracker.cloud.framework.contexts.xchannelrequestid.XChannelRequestIdContextObject;

import java.util.function.Supplier;

public class QuarkusXChannelRequestStrategyImpl extends AbstractXChannelRequestIdStrategy {
    private final Strategy<XChannelRequestIdContextObject> strategy;

    @SuppressWarnings("deprecation")
    public QuarkusXChannelRequestStrategyImpl(Supplier<XChannelRequestIdContextObject> defaultContextObject) {
        strategy = new RestEasyDefaultStrategy<>(XChannelRequestIdContextObject.class, defaultContextObject);
    }

    @Override
    public Strategy<XChannelRequestIdContextObject> getStrategy() {
        return strategy;
    }
}
