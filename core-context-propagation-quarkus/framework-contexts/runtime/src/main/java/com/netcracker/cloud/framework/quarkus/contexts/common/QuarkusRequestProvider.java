package com.netcracker.cloud.framework.quarkus.contexts.common;

import com.netcracker.cloud.context.propagation.core.RegisterProvider;
import com.netcracker.cloud.context.propagation.core.Strategy;
import com.netcracker.cloud.context.propagation.core.contexts.common.RequestContextObject;
import com.netcracker.cloud.context.propagation.core.contexts.common.RequestProvider;
import com.netcracker.cloud.context.propagation.core.supports.strategies.RestEasyDefaultStrategy;

@RegisterProvider
public class QuarkusRequestProvider extends RequestProvider {
    RestEasyDefaultStrategy<RequestContextObject> requestStrategy = new RestEasyDefaultStrategy<>(RequestContextObject.class, defaultContextObject());

    @Override
    public Strategy<RequestContextObject> strategy() {
        return requestStrategy;
    }

    @Override
    public int providerOrder() {
        return -100;
    }
}
