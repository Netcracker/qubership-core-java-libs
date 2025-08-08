package org.qubership.cloud.framework.quarkus.contexts.allowedheaders;

import org.qubership.cloud.context.propagation.core.RegisterProvider;
import org.qubership.cloud.context.propagation.core.Strategy;
import org.qubership.cloud.context.propagation.core.supports.strategies.RestEasyDefaultStrategy;
import org.qubership.cloud.framework.contexts.allowedheaders.AllowedHeadersContextObject;
import org.qubership.cloud.framework.contexts.allowedheaders.AllowedHeadersProvider;

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
