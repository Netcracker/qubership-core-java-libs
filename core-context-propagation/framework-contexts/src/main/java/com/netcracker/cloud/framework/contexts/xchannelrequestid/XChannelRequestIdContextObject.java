package com.netcracker.cloud.framework.contexts.xchannelrequestid;

import com.netcracker.cloud.context.propagation.core.contextdata.IncomingContextData;
import com.netcracker.cloud.context.propagation.core.contextdata.OutgoingContextData;
import com.netcracker.cloud.context.propagation.core.contexts.ResponsePropagatableContext;
import com.netcracker.cloud.context.propagation.core.contexts.SerializableContext;
import com.netcracker.cloud.context.propagation.core.contexts.SerializableDataContext;
import com.netcracker.cloud.framework.contexts.allowedheaders.HeaderPropagationConfiguration;
import org.jetbrains.annotations.Nullable;
import java.util.Collections;

import java.util.Map;

/**
 * Associates a given channel request id with the current execution thread. The purpose of the class is to provide a convenient
 * way to manage X_CHANNEL_REQUEST_ID header.
 */
public class XChannelRequestIdContextObject implements SerializableContext,
        ResponsePropagatableContext, SerializableDataContext {
    public static final String X_CHANNEL_REQUEST_ID = "X-Channel-Request-Id";

    private String channelRequestId;

    public XChannelRequestIdContextObject(@Nullable IncomingContextData contextData) {
        if (contextData != null && contextData.get(X_CHANNEL_REQUEST_ID) != null) {
            this.channelRequestId = (String) contextData.get(X_CHANNEL_REQUEST_ID);
        } else {
            this.channelRequestId = "-";
        }
    }

    public XChannelRequestIdContextObject(String channelRequestId) {
        this.channelRequestId = channelRequestId != null ? channelRequestId : "-";
    }

    @Override
    public void serialize(OutgoingContextData outgoingContextData) {
        if (!HeaderPropagationConfiguration.isBlacklisted(X_CHANNEL_REQUEST_ID)) {
            outgoingContextData.set(X_CHANNEL_REQUEST_ID, channelRequestId);
        }
    }

    public String getChannelRequestId() {
        return channelRequestId;
    }

    @Override
    public void propagate(OutgoingContextData outgoingContextData) {
        outgoingContextData.set(X_CHANNEL_REQUEST_ID, channelRequestId);
    }

    @Override
    public Map<String, Object> getSerializableContextData() {
        if (HeaderPropagationConfiguration.isBlacklisted(X_CHANNEL_REQUEST_ID)) {
            return Collections.emptyMap();
        }
        return Map.of(X_CHANNEL_REQUEST_ID, channelRequestId);
    }
}
