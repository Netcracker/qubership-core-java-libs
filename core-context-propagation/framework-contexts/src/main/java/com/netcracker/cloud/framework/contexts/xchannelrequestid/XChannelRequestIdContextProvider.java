package com.netcracker.cloud.framework.contexts.xchannelrequestid;

import com.netcracker.cloud.context.propagation.core.ContextInitializationStep;
import com.netcracker.cloud.context.propagation.core.ContextProvider;
import com.netcracker.cloud.context.propagation.core.RegisterProvider;
import com.netcracker.cloud.context.propagation.core.Strategy;
import com.netcracker.cloud.context.propagation.core.contextdata.IncomingContextData;
import org.jetbrains.annotations.Nullable;

@RegisterProvider
public class XChannelRequestIdContextProvider implements ContextProvider<XChannelRequestIdContextObject> {
    private final Strategy<XChannelRequestIdContextObject> xChannelRequestIdContextStrategy = new XChannelRequestIdStrategy(() -> provide(null));
    public static final String X_CHANNEL_REQUEST_ID_CONTEXT_NAME = "X-Channel-Request-Id";

    @Override
    public Strategy<XChannelRequestIdContextObject> strategy() {
        return xChannelRequestIdContextStrategy;
    }

    @Override
    public int initLevel() {
        return 0;
    }

    @Override
    public ContextInitializationStep getInitializationStep() {
        return ContextInitializationStep.PRE_AUTHENTICATION;
    }

    @Override
    public int providerOrder() {
        return 0;
    }

    @Override
    public final String contextName() {
        return X_CHANNEL_REQUEST_ID_CONTEXT_NAME;
    }

    @Override
    public XChannelRequestIdContextObject provide(@Nullable IncomingContextData incomingContextData) {
        return new XChannelRequestIdContextObject(incomingContextData);
    }
}
