package org.qubership.cloud.context.propagation.core.providers.initlevels;

import org.qubership.cloud.context.propagation.core.RegisterProvider;
import org.qubership.cloud.context.propagation.core.contextdata.IncomingContextData;
import org.qubership.cloud.context.propagation.core.supports.providers.AbstractContextProviderOnThreadLocal;
import org.jetbrains.annotations.Nullable;

@RegisterProvider
public class CheckInitLevelThree extends AbstractContextProviderOnThreadLocal<Object> {

    public static final String CONTEXT_NAME = "initLevelThree";

    @Override
    public int initLevel() {
        return -3;
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
