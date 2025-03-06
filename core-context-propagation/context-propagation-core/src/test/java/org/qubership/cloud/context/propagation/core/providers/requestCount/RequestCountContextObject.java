package org.qubership.cloud.context.propagation.core.providers.requestCount;

import org.qubership.cloud.context.propagation.core.contextdata.IncomingContextData;
import org.qubership.cloud.context.propagation.core.contextdata.OutgoingContextData;
import org.qubership.cloud.context.propagation.core.contexts.DefaultValueAwareContext;
import org.qubership.cloud.context.propagation.core.contexts.SerializableContext;
import org.qubership.cloud.context.propagation.core.contexts.SerializableDataContext;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class RequestCountContextObject implements SerializableContext, DefaultValueAwareContext, SerializableDataContext {
    private String requestCount;

    public static final String REQUEST_CONTEXT_SERIALIZATION_NAME = "Request";

    public RequestCountContextObject(String xVersion) {
        if (xVersion != null) {
            this.requestCount = xVersion;
        } else {
            this.requestCount = getDefault();
        }
    }

    public RequestCountContextObject(@Nullable IncomingContextData contextData) {
        if (contextData != null && contextData.get(REQUEST_CONTEXT_SERIALIZATION_NAME) != null) {
            this.requestCount = (String) contextData.get(REQUEST_CONTEXT_SERIALIZATION_NAME);
        } else {
            this.requestCount = getDefault();
        }
    }

    public RequestCountContextObject() {
        this.requestCount = getDefault();
    }

    @Override
    public void serialize(OutgoingContextData contextData) {
        contextData.set(REQUEST_CONTEXT_SERIALIZATION_NAME, requestCount);
    }

    public String getxVersion() {
        return requestCount;
    }

    public void setxVersion(String requestCount) {
        this.requestCount = requestCount;
    }

    @Override
    public String getDefault() {
        return "1";
    }

    @Override
    public Map<String, Object> getSerializableContextData() {
        return Map.of(REQUEST_CONTEXT_SERIALIZATION_NAME, requestCount);
    }
}
