package org.qubership.cloud.framework.quarkus.contexts.clientip;

import org.qubership.cloud.context.propagation.core.RegisterProvider;
import org.qubership.cloud.context.propagation.core.Strategy;
import org.qubership.cloud.framework.contexts.clientip.ClientIPContextObject;
import org.qubership.cloud.framework.contexts.clientip.ClientIPProvider;

@RegisterProvider
public class QuarkusClientIPProvider extends ClientIPProvider {
    private final Strategy<ClientIPContextObject> clientIPContextStrategy = new QuarkusClientIPStrategyImpl(() -> provide(null));

    @Override
    public Strategy<ClientIPContextObject> strategy() {
        return clientIPContextStrategy;
    }

    @Override
    public int providerOrder() {
        return -100;
    }
}
