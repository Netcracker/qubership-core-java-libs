package com.netcracker.cloud.framework.contexts.xrequestid;

import com.netcracker.cloud.context.propagation.core.ContextInitializationStep;
import com.netcracker.cloud.context.propagation.core.ContextProvider;
import com.netcracker.cloud.context.propagation.core.RegisterProvider;
import com.netcracker.cloud.context.propagation.core.Strategy;
import com.netcracker.cloud.context.propagation.core.contextdata.IncomingContextData;
import org.jetbrains.annotations.Nullable;

@RegisterProvider
public class XRequestIdContextProvider implements ContextProvider<XRequestIdContextObject> {
    private final Strategy<XRequestIdContextObject> xRequestIdContextStrategy = new XRequestIdStrategy(() -> provide(null));
    public static final String X_REQUEST_ID_CONTEXT_NAME = "X-Request-Id";

    @Override
    public Strategy<XRequestIdContextObject> strategy() {
        return xRequestIdContextStrategy;
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
        return X_REQUEST_ID_CONTEXT_NAME;
    }

    @Override
    public XRequestIdContextObject provide(@Nullable IncomingContextData incomingContextData) {
        return new XRequestIdContextObject(incomingContextData);
    }
}
