package org.qubership.cloud.framework.quarkus.contexts.xrequestid;

import org.qubership.cloud.context.propagation.core.RegisterProvider;
import org.qubership.cloud.context.propagation.core.Strategy;
import org.qubership.cloud.framework.contexts.xrequestid.XRequestIdContextObject;
import org.qubership.cloud.framework.contexts.xrequestid.XRequestIdContextProvider;

@RegisterProvider
public class QuarkusXRequestIdContextProvider extends XRequestIdContextProvider {
    private final Strategy<XRequestIdContextObject> xRequestIdContextStrategy = new QuarkusXRequestStrategyImpl(() -> provide(null));

    @Override
    public Strategy<XRequestIdContextObject> strategy() {
        return xRequestIdContextStrategy;
    }

    @Override
    public int providerOrder() {
        return -100;
    }

}
