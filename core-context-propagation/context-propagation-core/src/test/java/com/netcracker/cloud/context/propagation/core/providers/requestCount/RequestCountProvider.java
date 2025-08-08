package org.qubership.cloud.context.propagation.core.providers.requestCount;

import org.qubership.cloud.context.propagation.core.RegisterProvider;
import org.qubership.cloud.context.propagation.core.contextdata.IncomingContextData;
import org.qubership.cloud.context.propagation.core.supports.providers.AbstractContextProviderOnThreadLocal;
import org.jetbrains.annotations.Nullable;

@RegisterProvider
public class RequestCountProvider extends AbstractContextProviderOnThreadLocal<RequestCountContextObject> {

    public static final String CONTEXT_NAME = "requestContext";

    @Override
    public final String contextName() {
        return CONTEXT_NAME;
    }

    @Override
    public RequestCountContextObject provide(@Nullable IncomingContextData contextData) {
        return new RequestCountContextObject(contextData);
    }
}
