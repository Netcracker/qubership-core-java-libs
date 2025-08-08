package com.netcracker.cloud.framework.quarkus.contexts.apiversion;

import com.netcracker.cloud.context.propagation.core.RegisterProvider;
import com.netcracker.cloud.context.propagation.core.Strategy;
import com.netcracker.cloud.context.propagation.core.supports.strategies.RestEasyDefaultStrategy;
import com.netcracker.cloud.framework.contexts.apiversion.ApiVersionContextObject;
import com.netcracker.cloud.framework.contexts.apiversion.ApiVersionProvider;

@RegisterProvider
public class QuarkusApiVersionProvider extends ApiVersionProvider {

    RestEasyDefaultStrategy<ApiVersionContextObject> apiVersionStrategy = new RestEasyDefaultStrategy<>(ApiVersionContextObject.class, defaultContextObject());

    @Override
    public Strategy<ApiVersionContextObject> strategy() {
        return apiVersionStrategy;
    }

    @Override
    public int providerOrder() {
        return -100;
    }
}
