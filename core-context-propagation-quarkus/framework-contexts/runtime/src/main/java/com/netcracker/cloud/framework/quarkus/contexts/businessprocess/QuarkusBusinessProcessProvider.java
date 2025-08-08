package com.netcracker.cloud.framework.quarkus.contexts.businessprocess;

import com.netcracker.cloud.context.propagation.core.RegisterProvider;
import com.netcracker.cloud.context.propagation.core.Strategy;
import com.netcracker.cloud.context.propagation.core.supports.strategies.RestEasyDefaultStrategy;
import com.netcracker.cloud.framework.contexts.businessprocess.BusinessProcessContextObject;
import com.netcracker.cloud.framework.contexts.businessprocess.BusinessProcessProvider;

@RegisterProvider
public class QuarkusBusinessProcessProvider extends BusinessProcessProvider {

    RestEasyDefaultStrategy<BusinessProcessContextObject> businessProcessStrategy = new RestEasyDefaultStrategy<>(BusinessProcessContextObject.class, defaultContextObject());

    @Override
    public Strategy<BusinessProcessContextObject> strategy() {
        return businessProcessStrategy;
    }

    @Override
    public int providerOrder() {
        return -100;
    }
}
