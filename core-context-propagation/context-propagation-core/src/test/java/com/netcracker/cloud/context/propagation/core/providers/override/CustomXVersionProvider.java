package com.netcracker.cloud.context.propagation.core.providers.override;

import com.netcracker.cloud.context.propagation.core.Strategy;
import com.netcracker.cloud.context.propagation.core.providers.xversion.XVersionContextObject;
import com.netcracker.cloud.context.propagation.core.providers.xversion.XVersionProvider;

public class CustomXVersionProvider extends XVersionProvider {

    CustomXVersionStrategy customXVersionStrategy = new CustomXVersionStrategy();

    @Override
    public Strategy<XVersionContextObject> strategy() {
        return customXVersionStrategy;
    }

    @Override
    public int providerOrder() {
        return -1;
    }
}
