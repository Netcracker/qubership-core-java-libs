package com.netcracker.cloud.framework.contexts.xversionname;

import com.netcracker.cloud.context.propagation.core.RegisterProvider;
import com.netcracker.cloud.context.propagation.core.contextdata.IncomingContextData;
import com.netcracker.cloud.context.propagation.core.supports.providers.AbstractContextProviderOnInheritableThreadLocal;
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
