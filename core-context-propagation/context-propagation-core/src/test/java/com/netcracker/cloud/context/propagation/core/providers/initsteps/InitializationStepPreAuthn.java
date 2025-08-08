package com.netcracker.cloud.context.propagation.core.providers.initsteps;

import org.qubership.cloud.context.propagation.core.ContextInitializationStep;
import org.qubership.cloud.context.propagation.core.RegisterProvider;
import org.qubership.cloud.context.propagation.core.contextdata.IncomingContextData;
import org.qubership.cloud.context.propagation.core.supports.providers.AbstractContextProviderOnThreadLocal;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

@RegisterProvider
public class InitializationStepPreAuthn extends AbstractContextProviderOnThreadLocal<String> {

    public static final String CONTEXT_NAME = "InitStepPreAuthn";

    @Override
    public ContextInitializationStep getInitializationStep() {
        return ContextInitializationStep.PRE_AUTHENTICATION;
    }

    @Override
    public final String contextName() {
        return CONTEXT_NAME;
    }

    @Override
    public String provide(@Nullable IncomingContextData incomingContextData) {
        return CONTEXT_NAME;
    }

    @Override
    protected Supplier<String> defaultContextObject() {
        return () -> null;
    }
}
