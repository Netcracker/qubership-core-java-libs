package com.netcracker.cloud.framework.contexts.xversionname;

import org.qubership.cloud.context.propagation.core.RegisterProvider;
import org.qubership.cloud.context.propagation.core.contextdata.IncomingContextData;
import org.qubership.cloud.context.propagation.core.supports.providers.AbstractContextProviderOnInheritableThreadLocal;
import org.jetbrains.annotations.Nullable;

@RegisterProvider
public class XVersionNameProvider extends AbstractContextProviderOnInheritableThreadLocal<XVersionNameContextObject> {
    public static final String CONTEXT_NAME = "x-version-name";

    @Override
    public final String contextName() {
        return CONTEXT_NAME;
    }

    @Override
    public XVersionNameContextObject provide(@Nullable IncomingContextData contextData) {
        return new XVersionNameContextObject(contextData);
    }
}
