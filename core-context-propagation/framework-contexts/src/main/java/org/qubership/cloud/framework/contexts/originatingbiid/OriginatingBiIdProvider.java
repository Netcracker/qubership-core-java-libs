package org.qubership.cloud.framework.contexts.originatingbiid;

import org.qubership.cloud.context.propagation.core.ContextProvider;
import org.qubership.cloud.context.propagation.core.RegisterProvider;
import org.qubership.cloud.context.propagation.core.Strategy;
import org.qubership.cloud.context.propagation.core.contextdata.IncomingContextData;
import org.jetbrains.annotations.Nullable;

@RegisterProvider
public class OriginatingBiIdProvider implements ContextProvider<OriginatingBiIdContextObject> {
    private final Strategy<OriginatingBiIdContextObject> originatingBiIdStrategy = new OriginatingBiIdStrategy(() -> provide(null));
    public static final String CONTEXT_NAME = "originating-bi-id";

    @Override
    public Strategy<OriginatingBiIdContextObject> strategy() {
        return originatingBiIdStrategy;
    }

    @Override
    public int initLevel() {
        return 0;
    }

    @Override
    public int providerOrder() {
        return 0;
    }

    @Override
    public final String contextName() {
        return CONTEXT_NAME;
    }

    @Override
    public OriginatingBiIdContextObject provide(@Nullable IncomingContextData contextData) {
        return new OriginatingBiIdContextObject(contextData);
    }
}
