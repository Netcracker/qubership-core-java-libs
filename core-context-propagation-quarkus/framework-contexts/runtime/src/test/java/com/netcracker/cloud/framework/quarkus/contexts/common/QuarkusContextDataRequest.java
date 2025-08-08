package com.netcracker.cloud.framework.quarkus.contexts.common;

import com.netcracker.cloud.context.propagation.core.contextdata.IncomingContextData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuarkusContextDataRequest implements IncomingContextData {
    Map<String, Object> contextDataMap = new HashMap<>();

    public QuarkusContextDataRequest() {}

    public QuarkusContextDataRequest(String key, Object value) {
        contextDataMap.put(key, value);
    }

    @Override
    public Object get(String name) {
        return contextDataMap.get(name);
    }

    @Override
    public Map<String, List<?>> getAll() {
        return null;
    }
}
