package com.netcracker.cloud.framework.contexts.strategies;

import org.qubership.cloud.context.propagation.core.Strategy;
import org.qubership.cloud.context.propagation.core.contextdata.IncomingContextData;
import org.qubership.cloud.context.propagation.core.supports.strategies.AbstractStrategy;
import org.qubership.cloud.framework.contexts.xrequestid.XRequestIdContextObject;
import org.jetbrains.annotations.Nullable;
import org.slf4j.MDC;

import java.util.function.Supplier;

public abstract class AbstractXRequestIdStrategy extends AbstractStrategy<XRequestIdContextObject> {
    public abstract Strategy<XRequestIdContextObject> getStrategy();

    public static final String MDC_REQUEST_ID_KEY = "requestId";

    private static final IncomingContextData nullContextData = null;
    private static final XRequestIdContextObject DEFAULT_VALUE = new XRequestIdContextObject(nullContextData);

    @Override
    public void clear() {
        getStrategy().clear();
        MDC.remove(MDC_REQUEST_ID_KEY);
    }

    @Override
    public void set(XRequestIdContextObject value) {
        getStrategy().set(value);
        MDC.put(MDC_REQUEST_ID_KEY, value.getRequestId());
    }

    @Override
    public XRequestIdContextObject get() {
        XRequestIdContextObject xRequestIdContextObject = getStrategy().get();
        MDC.put(MDC_REQUEST_ID_KEY, xRequestIdContextObject.getRequestId());
        return xRequestIdContextObject;
    }

    @Override
    public boolean isValid(@Nullable XRequestIdContextObject value) {
        return value != null;
    }

    @Override
    protected Supplier<XRequestIdContextObject> defaultObjectSupplier() {
        return () -> DEFAULT_VALUE;
    }
}
