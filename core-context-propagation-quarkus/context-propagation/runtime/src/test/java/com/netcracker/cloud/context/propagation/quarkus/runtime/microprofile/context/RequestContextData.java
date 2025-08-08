package com.netcracker.cloud.context.propagation.quarkus.runtime.microprofile.context;

import org.qubership.cloud.context.propagation.core.contextdata.IncomingContextData;

import java.util.List;
import java.util.Map;

public class RequestContextData implements IncomingContextData {
    private final Map<String, Object> contextData;

    public RequestContextData(Map<String, Object> contextData) {
        this.contextData = contextData;
    }

    @Override
    public Object get(String s) {
        return contextData.get(s);
    }

    @Override
    public Map<String, List<?>> getAll() {
        return null;
    }
}
