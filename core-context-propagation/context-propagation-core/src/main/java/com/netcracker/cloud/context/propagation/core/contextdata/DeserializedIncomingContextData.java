package com.netcracker.cloud.context.propagation.core.contextdata;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class DeserializedIncomingContextData implements IncomingContextData {

    private final Map<String, Object> data = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    public DeserializedIncomingContextData(Map<String, Object> data) {
        this.data.putAll(data);
    }

    @Override
    public Object get(String name) {
        return data.get(name);
    }

    @Override
    public Map<String, List<?>> getAll() {
        throw new UnsupportedOperationException("method is deprecate and not supported");
    }
}
