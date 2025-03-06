package org.qubership.cloud.context.propagation.core.contextdata;

import java.util.List;
import java.util.Map;

public class DeserializedIncomingContextData implements IncomingContextData {

    private Map<String, Object> data;

    public DeserializedIncomingContextData(Map<String, Object> data) {
        this.data = data;
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
