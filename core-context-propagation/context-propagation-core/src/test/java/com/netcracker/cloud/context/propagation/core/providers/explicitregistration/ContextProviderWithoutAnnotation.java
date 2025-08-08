package org.qubership.cloud.context.propagation.core.providers.explicitregistration;

import org.qubership.cloud.context.propagation.core.contextdata.IncomingContextData;
import org.qubership.cloud.context.propagation.core.supports.providers.AbstractContextProviderOnThreadLocal;
import org.jetbrains.annotations.Nullable;

//@RegisterProvider
public class ContextProviderWithoutAnnotation extends AbstractContextProviderOnThreadLocal<Object> {

    public static final String CONTEXT_PROVIDER_WITHOUT_ANNOTATION = "contextProviderWithoutAnnotation";

    @Override
    public final String contextName() {
        return CONTEXT_PROVIDER_WITHOUT_ANNOTATION;
    }

    @Override
    public Object provide(@Nullable IncomingContextData incomingContextData) {
        return null;
    }
}
