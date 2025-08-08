package com.netcracker.cloud.framework.contexts.xversionname;

import com.netcracker.cloud.context.propagation.core.contextdata.IncomingContextData;
import com.netcracker.cloud.context.propagation.core.contextdata.OutgoingContextData;
import com.netcracker.cloud.context.propagation.core.contexts.DefaultValueAwareContext;
import com.netcracker.cloud.context.propagation.core.contexts.SerializableContext;
import com.netcracker.cloud.context.propagation.core.contexts.SerializableDataContext;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;

public class XVersionNameContextObject implements SerializableContext, DefaultValueAwareContext<String>, SerializableDataContext {
    public static final String X_VERSION_NAME_SERIALIZATION_NAME = "X-Version-Name";
    private final String xVersionName;

    public XVersionNameContextObject(@Nullable IncomingContextData contextData) {
        if (contextData != null && contextData.get(X_VERSION_NAME_SERIALIZATION_NAME) != null) {
            this.xVersionName = (String) contextData.get(X_VERSION_NAME_SERIALIZATION_NAME);
        } else {
            this.xVersionName = null;
        }
    }

    public XVersionNameContextObject(String xVersionName) {
        this.xVersionName = xVersionName;
    }

    @Override
    public void serialize(OutgoingContextData contextData) {
        final String actualXVersionNameValue = getXVersionName();
        if (StringUtils.isNotBlank(actualXVersionNameValue)) {
            contextData.set(X_VERSION_NAME_SERIALIZATION_NAME, actualXVersionNameValue);
        }
    }

    public String getXVersionName() {
        return StringUtils.isBlank(xVersionName)? getDefault() : xVersionName;
    }

    @Override
    public String getDefault() {
        return null;
    }

    @Override
    public Map<String, Object> getSerializableContextData() {
        String xVersionName= getXVersionName();
        if (StringUtils.isNotBlank(xVersionName)) {
            return Map.of(X_VERSION_NAME_SERIALIZATION_NAME, xVersionName);
        }
        return Collections.emptyMap();
    }
}
