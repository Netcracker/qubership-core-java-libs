package com.netcracker.cloud.framework.contexts.xchannelrequestid;

import com.netcracker.cloud.context.propagation.core.Strategy;
import com.netcracker.cloud.context.propagation.core.supports.strategies.DefaultStrategies;
import com.netcracker.cloud.framework.contexts.strategies.AbstractXChannelRequestIdStrategy;

import java.util.function.Supplier;

public class XChannelRequestIdStrategy extends AbstractXChannelRequestIdStrategy {

    private final Strategy<XChannelRequestIdContextObject> strategy;

    public XChannelRequestIdStrategy(Supplier<XChannelRequestIdContextObject> defaultContextObject) {
        strategy = DefaultStrategies.threadLocalWithInheritanceDefaultStrategy(defaultContextObject);
    }

    @Override
    public Strategy<XChannelRequestIdContextObject> getStrategy() {
        return strategy;
    }

}
