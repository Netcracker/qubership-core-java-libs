package com.netcracker.cloud.framework.contexts.strategies;

import org.qubership.cloud.context.propagation.core.Strategy;
import org.qubership.cloud.context.propagation.core.contextdata.IncomingContextData;
import org.qubership.cloud.context.propagation.core.supports.strategies.AbstractStrategy;
import org.qubership.cloud.framework.contexts.originatingbiid.OriginatingBiIdContextObject;
import org.jetbrains.annotations.Nullable;
import org.slf4j.MDC;

import java.util.function.Supplier;

public abstract class AbstractOriginatingBiIdStrategy extends AbstractStrategy<OriginatingBiIdContextObject> {
    public abstract Strategy<OriginatingBiIdContextObject> getStrategy();

    public static final String MDC_REQUEST_ID_KEY = "originating_bi_id";

    private static final IncomingContextData nullContextData = null;
    private static final OriginatingBiIdContextObject DEFAULT_VALUE = new OriginatingBiIdContextObject(nullContextData);

    @Override
    public void clear() {
        getStrategy().clear();
        MDC.remove(MDC_REQUEST_ID_KEY);
    }

    @Override
    public void set(OriginatingBiIdContextObject value) {
        getStrategy().set(value);
        if (value != null && value.getOriginatingBiId() != null) {
            MDC.put(MDC_REQUEST_ID_KEY, value.getOriginatingBiId());
        } else {
            MDC.remove(MDC_REQUEST_ID_KEY);
        }
    }

    @Override
    public OriginatingBiIdContextObject get() {
        return getStrategy().get();
    }

    @Override
    public boolean isValid(@Nullable OriginatingBiIdContextObject value) {
        return value != null;
    }

    @Override
    protected Supplier<OriginatingBiIdContextObject> defaultObjectSupplier() {
        return () -> DEFAULT_VALUE;
    }
}
