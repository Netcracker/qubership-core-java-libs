package com.netcracker.cloud.framework.contexts.xversion;

import org.qubership.cloud.context.propagation.core.contextdata.IncomingContextData;
import org.qubership.cloud.context.propagation.core.contextdata.OutgoingContextData;
import org.qubership.cloud.context.propagation.core.contexts.DefaultValueAwareContext;
import org.qubership.cloud.context.propagation.core.contexts.SerializableContext;
import org.qubership.cloud.context.propagation.core.contexts.SerializableDataContext;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;

public class XVersionContextObject implements SerializableContext, DefaultValueAwareContext<String>, SerializableDataContext {
    public static final String X_VERSION_SERIALIZATION_NAME = "X-Version";
    private final String xVersion;

    public XVersionContextObject(@Nullable IncomingContextData contextData) {
        if (contextData != null && contextData.get(X_VERSION_SERIALIZATION_NAME) != null) {
            this.xVersion = (String) contextData.get(X_VERSION_SERIALIZATION_NAME);
        } else {
            this.xVersion = "";
        }
    }

    @Override
    public void serialize(OutgoingContextData contextData) {
        final String actualXVersionValue = getXVersion();
        if (StringUtils.isNotBlank(actualXVersionValue)) {
            contextData.set(X_VERSION_SERIALIZATION_NAME, actualXVersionValue);
        }
    }

    public String getXVersion() {
        return StringUtils.isBlank(xVersion)? getDefault() : xVersion;
    }

    @Override
    public String getDefault() {
        return "";
    }

    @Override
    public Map<String, Object> getSerializableContextData() {
        String version = getXVersion();
        if (StringUtils.isNotBlank(version)) {
            return Map.of(X_VERSION_SERIALIZATION_NAME, version);
        }
        return Collections.emptyMap();
    }
}
