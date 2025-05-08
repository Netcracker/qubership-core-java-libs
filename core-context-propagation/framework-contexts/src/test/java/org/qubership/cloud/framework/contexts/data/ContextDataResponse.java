package org.qubership.cloud.framework.contexts.data;

import org.qubership.cloud.context.propagation.core.contextdata.OutgoingContextData;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class ContextDataResponse implements OutgoingContextData {

    private Map<String, Object> responseHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    @Override
    public void set(String name, Object values) {
        responseHeaders.put(name, values);
    }

    public Map<String, Object> getResponseHeaders() {
        return responseHeaders;
    }
}
