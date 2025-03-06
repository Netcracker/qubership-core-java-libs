package org.qubership.cloud.framework.contexts.data;

import org.qubership.cloud.context.propagation.core.contextdata.OutgoingContextData;

import java.util.HashMap;
import java.util.Map;

public class ContextDataResponse implements OutgoingContextData {

    private Map<String, Object> responseHeaders = new HashMap<>();

    @Override
    public void set(String name, Object values) {
        responseHeaders.put(name, values);
    }

    public Map<String, Object> getResponseHeaders() {
        return responseHeaders;
    }
}
