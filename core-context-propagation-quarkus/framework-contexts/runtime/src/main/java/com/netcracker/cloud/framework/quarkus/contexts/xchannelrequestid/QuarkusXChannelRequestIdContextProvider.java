package com.netcracker.cloud.framework.quarkus.contexts.xchannelrequestid;

import com.netcracker.cloud.context.propagation.core.RegisterProvider;
import com.netcracker.cloud.context.propagation.core.Strategy;
import com.netcracker.cloud.framework.contexts.xchannelrequestid.XChannelRequestIdContextObject;
import com.netcracker.cloud.framework.contexts.xchannelrequestid.XChannelRequestIdContextProvider;

@RegisterProvider
public class QuarkusXChannelRequestIdContextProvider extends XChannelRequestIdContextProvider {
    private final Strategy<XChannelRequestIdContextObject> xChannelRequestIdContextStrategy =
            new QuarkusXChannelRequestStrategyImpl(() -> provide(null));

    @Override
    public Strategy<XChannelRequestIdContextObject> strategy() {
        return xChannelRequestIdContextStrategy;
    }

    @Override
    public int providerOrder() {
        return -100;
    }
}
