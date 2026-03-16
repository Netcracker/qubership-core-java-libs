package com.netcracker.cloud.framework.quarkus.contexts.allowedheaders;

import com.netcracker.cloud.context.propagation.core.RegisterProvider;
import com.netcracker.cloud.context.propagation.core.Strategy;
import com.netcracker.cloud.context.propagation.core.supports.strategies.RestEasyDefaultStrategy;
import com.netcracker.cloud.framework.contexts.allowedheaders.AllowedHeadersContextObject;
import com.netcracker.cloud.framework.contexts.allowedheaders.AllowedHeadersProvider;

@RegisterProvider
public class QuarkusAllowedHeadersProvider extends AllowedHeadersProvider {

    RestEasyDefaultStrategy<AllowedHeadersContextObject> allowedHeadersStrategy = new RestEasyDefaultStrategy<>(AllowedHeadersContextObject.class, defaultContextObject());

    @Override
    public Strategy<AllowedHeadersContextObject> strategy() {
        return allowedHeadersStrategy;
    }

    @Override
    public int providerOrder() {
        return -100;
    }

}
