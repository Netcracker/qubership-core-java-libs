package com.foo.bar;

import org.jetbrains.annotations.Nullable;
import org.qubership.cloud.context.propagation.core.RegisterProvider;
import org.qubership.cloud.context.propagation.core.contextdata.IncomingContextData;
import org.qubership.cloud.context.propagation.core.supports.providers.AbstractContextProviderOnThreadLocal;

@RegisterProvider
public class ComSampleContext extends AbstractContextProviderOnThreadLocal<Object> {
    public static final String CONTEXT_NAME = "ComSampleContext";

    @Override
    public int initLevel() {
        return -1;
    }

    @Override
    public final String contextName() {
        return CONTEXT_NAME;
    }

    @Override
    public Object provide(@Nullable IncomingContextData incomingContextData) {
        return null;
    }
}
