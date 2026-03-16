package com.netcracker.cloud.framework.quarkus.contexts.xversionname;

import com.netcracker.cloud.context.propagation.core.RegisterProvider;
import com.netcracker.cloud.context.propagation.core.Strategy;
import com.netcracker.cloud.context.propagation.core.supports.strategies.RestEasyDefaultStrategy;
import com.netcracker.cloud.framework.contexts.xversionname.XVersionNameContextObject;
import com.netcracker.cloud.framework.contexts.xversionname.XVersionNameProvider;

@RegisterProvider
public class QuarkusXVersionNameProvider extends XVersionNameProvider {

    RestEasyDefaultStrategy<XVersionNameContextObject> xVersionNameStrategy = new RestEasyDefaultStrategy<>(XVersionNameContextObject.class, defaultContextObject());

    @Override
    public Strategy<XVersionNameContextObject> strategy() {
        return xVersionNameStrategy;
    }

    @Override
    public int providerOrder() {
        return -100;
    }
}
