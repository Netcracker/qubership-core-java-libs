package org.qubership.cloud.framework.contexts.data;

import org.qubership.cloud.context.propagation.core.contextdata.IncomingContextData;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static jakarta.ws.rs.core.HttpHeaders.ACCEPT_LANGUAGE;

public class ContextDataRequest implements IncomingContextData {

    Map<String, Object> contextDataMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    public static final String CUSTOM_HEADER = "Custom-header-1";
    private static final String WRONG_CUSTOM_HEADER = "Custom-header-2";
    private static final String URL_HEADER = "cloud-core.context-propagation.url";

    public ContextDataRequest() {
        contextDataMap.put(ACCEPT_LANGUAGE, "en; ru;");
        contextDataMap.put(URL_HEADER, "api/v2/some-test-url");
        contextDataMap.put(WRONG_CUSTOM_HEADER, "tmp-value");
        contextDataMap.put(CUSTOM_HEADER, "custom_value");
    }

    public ContextDataRequest(String headerName, Object headerValue) {
        this();
        contextDataMap.put(headerName, headerValue);
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
