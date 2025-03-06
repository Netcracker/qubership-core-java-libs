package org.qubership.cloud.context.propagation.core.contexts.common;

import org.qubership.cloud.context.propagation.core.RegisterProvider;
import org.qubership.cloud.context.propagation.core.Strategy;
import org.qubership.cloud.context.propagation.core.contextdata.IncomingContextData;
import org.qubership.cloud.context.propagation.core.supports.providers.AbstractContextProvider;
import org.qubership.cloud.context.propagation.core.supports.strategies.DefaultStrategies;
import org.jetbrains.annotations.Nullable;

@RegisterProvider
public class RequestProvider extends AbstractContextProvider<RequestContextObject> {

    public static final String REQUEST_CONTEXT_NAME = "request";
    private final Strategy<RequestContextObject> strategy = DefaultStrategies.threadLocalWithInheritanceDefaultStrategy(() -> provide(null));

    @Override
    public Strategy<RequestContextObject> strategy() {
        return strategy;
    }

    @Override
    public final String contextName() {
        return REQUEST_CONTEXT_NAME;
    }

    @Override
    public RequestContextObject provide(@Nullable IncomingContextData incomingContextData) {
        return new RequestContextObject(incomingContextData);
    }
}