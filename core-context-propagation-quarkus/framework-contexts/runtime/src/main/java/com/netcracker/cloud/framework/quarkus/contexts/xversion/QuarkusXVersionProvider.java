package com.netcracker.cloud.framework.quarkus.contexts.xversion;

import com.netcracker.cloud.context.propagation.core.RegisterProvider;
import com.netcracker.cloud.context.propagation.core.Strategy;
import com.netcracker.cloud.context.propagation.core.supports.strategies.RestEasyDefaultStrategy;
import com.netcracker.cloud.framework.contexts.xversion.XVersionContextObject;
import com.netcracker.cloud.framework.contexts.xversion.XVersionProvider;

@RegisterProvider
public class QuarkusXVersionProvider extends XVersionProvider {

    RestEasyDefaultStrategy<XVersionContextObject> xVersionStrategy = new RestEasyDefaultStrategy<>(XVersionContextObject.class, defaultContextObject());

    @Override
    public Strategy<XVersionContextObject> strategy() {
        return xVersionStrategy;
    }

    @Override
    public int providerOrder() {
        return -100;
    }

}
