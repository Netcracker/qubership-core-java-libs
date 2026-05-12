package com.netcracker.cloud.headerstracking.filters.context;

import com.netcracker.cloud.context.propagation.core.ContextManager;
import com.netcracker.cloud.framework.contexts.xchannelrequestid.XChannelRequestIdContextObject;
import org.jetbrains.annotations.NotNull;

import static com.netcracker.cloud.framework.contexts.xchannelrequestid.XChannelRequestIdContextObject.X_CHANNEL_REQUEST_ID;


public class ChannelRequestIdContext {

    private ChannelRequestIdContext() {
    }
    
    public static String get() {
        XChannelRequestIdContextObject xChannelRequestIdContextObject = ContextManager.get(X_CHANNEL_REQUEST_ID);
        return xChannelRequestIdContextObject.getChannelRequestId();
    }

    public static void set(@NotNull String newChannelRequestId) {
        XChannelRequestIdContextObject xChannelRequestIdContextObject = new XChannelRequestIdContextObject(newChannelRequestId);
        ContextManager.set(X_CHANNEL_REQUEST_ID, xChannelRequestIdContextObject);
    }

    public static void clear() {
        ContextManager.clear(X_CHANNEL_REQUEST_ID);
    }
}