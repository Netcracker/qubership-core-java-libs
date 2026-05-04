package com.netcracker.cloud.framework.contexts.strategies;

import com.netcracker.cloud.context.propagation.core.Strategy;
import com.netcracker.cloud.context.propagation.core.contextdata.IncomingContextData;
import com.netcracker.cloud.context.propagation.core.supports.strategies.AbstractStrategy;
import com.netcracker.cloud.framework.contexts.xchannelrequestid.XChannelRequestIdContextObject;
import org.jetbrains.annotations.Nullable;
import org.slf4j.MDC;

import java.util.function.Supplier;

public abstract class AbstractXChannelRequestIdStrategy extends AbstractStrategy<XChannelRequestIdContextObject> {
    public abstract Strategy<XChannelRequestIdContextObject> getStrategy();

    public static final String MDC_CHANNEL_REQUEST_ID_KEY = "x_channel_request_id";

    @Override
    public void clear() {
        getStrategy().clear();
        MDC.remove(MDC_CHANNEL_REQUEST_ID_KEY);
    }

    @Override
    public void set(XChannelRequestIdContextObject value) {
        getStrategy().set(value);
        MDC.put(MDC_CHANNEL_REQUEST_ID_KEY, value.getChannelRequestId());
    }

    @Override
    public XChannelRequestIdContextObject get() {
        XChannelRequestIdContextObject xChannelRequestIdContextObject = getStrategy().get();
        MDC.put(MDC_CHANNEL_REQUEST_ID_KEY, xChannelRequestIdContextObject.getChannelRequestId() != null 
            ? xChannelRequestIdContextObject.getChannelRequestId() 
            : "-");
        return xChannelRequestIdContextObject;
    }

    @Override
    public boolean isValid(@Nullable XChannelRequestIdContextObject value) {
        return value != null;
    }

    @Override
    protected Supplier<XChannelRequestIdContextObject> defaultObjectSupplier() {
        return () -> new XChannelRequestIdContextObject((IncomingContextData) null);
    }
}
