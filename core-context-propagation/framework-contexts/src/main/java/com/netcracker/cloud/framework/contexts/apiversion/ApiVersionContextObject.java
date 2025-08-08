package com.netcracker.cloud.framework.contexts.apiversion;

import org.qubership.cloud.context.propagation.core.contextdata.IncomingContextData;
import org.qubership.cloud.context.propagation.core.contexts.DefaultValueAwareContext;
import org.qubership.cloud.context.propagation.core.contexts.SerializableDataContext;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class ApiVersionContextObject implements DefaultValueAwareContext, SerializableDataContext {

    private String version;
    private static final String DEFAULT_VERSION = "v1";
    private static final String VERSION_URL = "v";
    private static final String URL_HEADER = "cloud-core.context-propagation.url";
    protected static final String SERIALIZED_API_VERSION = "api-version";

    public ApiVersionContextObject(@Nullable IncomingContextData contextData) {
        if (contextData != null && contextData.get(URL_HEADER) != null) {
            String apiVersion = StringUtils.substringBetween((String) contextData.get(URL_HEADER), "/" + VERSION_URL, "/");
            this.version = VERSION_URL + apiVersion;
        } else {
            this.version = getDefault();
        }
    }

    public ApiVersionContextObject(String version) {
        this.version = version;
    }

    public String getDefault() {
        return DEFAULT_VERSION;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public Map<String, Object> getSerializableContextData() {
        return Map.of(SERIALIZED_API_VERSION, version);
    }
}

