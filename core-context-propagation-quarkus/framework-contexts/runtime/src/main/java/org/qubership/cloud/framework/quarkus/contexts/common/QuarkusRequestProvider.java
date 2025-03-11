package org.qubership.cloud.framework.quarkus.contexts.common;

import org.qubership.cloud.context.propagation.core.RegisterProvider;
import org.qubership.cloud.context.propagation.core.Strategy;
import org.qubership.cloud.context.propagation.core.contexts.common.RequestContextObject;
import org.qubership.cloud.context.propagation.core.contexts.common.RequestProvider;
import org.qubership.cloud.context.propagation.core.supports.strategies.RestEasyDefaultStrategy;

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
