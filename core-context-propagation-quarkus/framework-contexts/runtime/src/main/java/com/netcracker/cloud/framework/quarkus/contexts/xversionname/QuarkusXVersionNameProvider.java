package com.netcracker.cloud.framework.quarkus.contexts.xversionname;

import org.qubership.cloud.context.propagation.core.RegisterProvider;
import org.qubership.cloud.context.propagation.core.Strategy;
import org.qubership.cloud.context.propagation.core.supports.strategies.RestEasyDefaultStrategy;
import org.qubership.cloud.framework.contexts.xversionname.XVersionNameContextObject;
import org.qubership.cloud.framework.contexts.xversionname.XVersionNameProvider;

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
