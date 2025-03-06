package org.qubership.cloud.context.propagation.core.providers.xversion;

import org.qubership.cloud.context.propagation.core.contextdata.IncomingContextData;
import org.qubership.cloud.context.propagation.core.contextdata.OutgoingContextData;
import org.qubership.cloud.context.propagation.core.contexts.DefaultValueAwareContext;
import org.qubership.cloud.context.propagation.core.contexts.SerializableContext;
import org.qubership.cloud.context.propagation.core.contexts.SerializableDataContext;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class XVersionContextObject implements SerializableContext,
        DefaultValueAwareContext, SerializableDataContext {

    private String xVersion;

    public static final String X_VERSION_SERIALIZATION_NAME = "X-Version";

    public XVersionContextObject(String xVersion) {
        if (xVersion != null) {
            this.xVersion = xVersion;
        } else {
            this.xVersion = getDefault();
        }
    }

    public XVersionContextObject(@Nullable IncomingContextData contextData) {
        if (contextData != null && contextData.get(X_VERSION_SERIALIZATION_NAME) != null) {
            this.xVersion = (String) contextData.get(X_VERSION_SERIALIZATION_NAME);
        } else {
            this.xVersion = getDefault();
        }
    }

    public XVersionContextObject() {
        this.xVersion = getDefault();
    }

    @Override
    public void serialize(OutgoingContextData contextData) {
        contextData.set(X_VERSION_SERIALIZATION_NAME, xVersion);
    }

    public String getxVersion() {
        return xVersion;
    }

    public void setxVersion(String xVersion) {
        this.xVersion = xVersion;
    }

    @Override
    public String getDefault() {
        return "v1";
    }

    @Override
    public Map<String, Object> getSerializableContextData() {
        return Map.of(X_VERSION_SERIALIZATION_NAME, xVersion);
    }
}
