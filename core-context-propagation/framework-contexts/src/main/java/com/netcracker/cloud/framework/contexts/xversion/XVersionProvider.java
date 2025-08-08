package com.netcracker.cloud.framework.contexts.xversion;

import com.netcracker.cloud.context.propagation.core.RegisterProvider;
import com.netcracker.cloud.context.propagation.core.contextdata.IncomingContextData;
import com.netcracker.cloud.context.propagation.core.supports.providers.AbstractContextProviderOnInheritableThreadLocal;
import org.jetbrains.annotations.Nullable;

@RegisterProvider
public class XVersionProvider extends AbstractContextProviderOnInheritableThreadLocal<XVersionContextObject> {
    public static final String CONTEXT_NAME = "x-version";

    @Override
    public final String contextName() {
        return CONTEXT_NAME;
    }

    @Override
    public XVersionContextObject provide(@Nullable IncomingContextData contextData) {
        return new XVersionContextObject(contextData);
    }
}
