package com.netcracker.cloud.framework.contexts.clientip;

import com.netcracker.cloud.context.propagation.core.ContextProvider;
import com.netcracker.cloud.context.propagation.core.RegisterProvider;
import com.netcracker.cloud.context.propagation.core.Strategy;
import com.netcracker.cloud.context.propagation.core.contextdata.IncomingContextData;
import org.jetbrains.annotations.Nullable;

import static com.netcracker.cloud.framework.contexts.clientip.ClientIPContextObject.X_NC_CLIENT_IP;

@RegisterProvider
public class ClientIPProvider implements ContextProvider<ClientIPContextObject> {
    private final Strategy<ClientIPContextObject> clientIPContextStrategy = new ClientIPStrategy(() -> provide(null));
    public static final String CONTEXT_NAME = X_NC_CLIENT_IP;

    @Override
    public Strategy<ClientIPContextObject> strategy() {
        return clientIPContextStrategy;
    }

    @Override
    public int initLevel() {
        return 0;
    }

    @Override
    public int providerOrder() {
        return 0;
    }

    @Override
    public String contextName() {
        return CONTEXT_NAME;
    }

    @Override
    public ClientIPContextObject provide(@Nullable IncomingContextData incomingContextData) {
        return new ClientIPContextObject(incomingContextData);
    }
}
