package com.netcracker.cloud.context.propagation.core.providers.initlevels;

import com.netcracker.cloud.context.propagation.core.RegisterProvider;
import com.netcracker.cloud.context.propagation.core.contextdata.IncomingContextData;
import com.netcracker.cloud.context.propagation.core.supports.providers.AbstractContextProviderOnThreadLocal;
import org.jetbrains.annotations.Nullable;

@RegisterProvider
public class CheckInitLevelTwo extends AbstractContextProviderOnThreadLocal<Object> {

    public static final String CONTEXT_NAME = "initLevelTwo";

    @Override
    public int initLevel() {
        return -2;
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
