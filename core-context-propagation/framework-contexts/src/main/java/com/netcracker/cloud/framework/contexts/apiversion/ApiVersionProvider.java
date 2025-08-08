package com.netcracker.cloud.framework.contexts.apiversion;

import org.qubership.cloud.context.propagation.core.RegisterProvider;
import org.qubership.cloud.context.propagation.core.contextdata.IncomingContextData;
import org.qubership.cloud.context.propagation.core.supports.providers.AbstractContextProviderOnInheritableThreadLocal;
import org.jetbrains.annotations.Nullable;

@RegisterProvider
public class ApiVersionProvider extends AbstractContextProviderOnInheritableThreadLocal<ApiVersionContextObject> {
    public static final String API_VERSION_CONTEXT_NAME = "Api-Version-Context";

    @Override
    public final String contextName() {
        return API_VERSION_CONTEXT_NAME;
    }

    @Override
    public ApiVersionContextObject provide(@Nullable IncomingContextData incomingContextData) {
        return new ApiVersionContextObject(incomingContextData);
    }

    @Override
    public ApiVersionContextObject provideFromSerializableData(IncomingContextData incomingContextData) {
        return new ApiVersionContextObject((String) incomingContextData.get(ApiVersionContextObject.SERIALIZED_API_VERSION));
    }
}
