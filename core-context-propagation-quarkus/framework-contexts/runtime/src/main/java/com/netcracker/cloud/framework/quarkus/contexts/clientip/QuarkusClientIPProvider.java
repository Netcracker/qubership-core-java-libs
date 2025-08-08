package com.netcracker.cloud.framework.quarkus.contexts.clientip;

import com.netcracker.cloud.context.propagation.core.RegisterProvider;
import com.netcracker.cloud.context.propagation.core.Strategy;
import com.netcracker.cloud.framework.contexts.clientip.ClientIPContextObject;
import com.netcracker.cloud.framework.contexts.clientip.ClientIPProvider;

@RegisterProvider
public class QuarkusClientIPProvider extends ClientIPProvider {
    private final Strategy<ClientIPContextObject> clientIPContextStrategy = new QuarkusClientIPStrategyImpl(() -> provide(null));

    @Override
    public Strategy<ClientIPContextObject> strategy() {
        return clientIPContextStrategy;
    }

    @Override
    public int providerOrder() {
        return -100;
    }
}
