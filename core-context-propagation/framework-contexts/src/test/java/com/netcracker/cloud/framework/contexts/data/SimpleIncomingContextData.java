package com.netcracker.cloud.framework.contexts.data;

import com.netcracker.cloud.context.propagation.core.contextdata.IncomingContextData;

import java.util.List;
import java.util.Map;

public class SimpleIncomingContextData implements IncomingContextData {

    private final Map<String, Object> data;

    public SimpleIncomingContextData(Map<String, Object> data) {
        this.data = data;
    }

    public SimpleIncomingContextData() {
        this.data = null;
    }

    @Override
    public Object get(String name) {
        if (data != null) {
            return data.get(name);
        }
        return null;
    }

    @Override
    public Map<String, List<?>> getAll() {
        return null;
    }
}
