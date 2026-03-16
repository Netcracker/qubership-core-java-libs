package com.netcracker.cloud.smartclient.rest.webclient.interceptor;

import com.netcracker.cloud.context.propagation.core.contextdata.IncomingContextData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.netcracker.cloud.framework.contexts.xrequestid.XRequestIdContextProvider.X_REQUEST_ID_CONTEXT_NAME;
import static jakarta.ws.rs.core.HttpHeaders.ACCEPT_LANGUAGE;

public class ContextDataRequest implements IncomingContextData {

    Map<String, Object> contextDataMap = new HashMap<>();

    public static final String ALLOWED_HEADER = "allowed_header";
    private static final String WRONG_CUSTOM_HEADER = "Custom-header-2";
    private static final String URL_HEADER = "cloud-core.context-propagation.url";

    public ContextDataRequest(){
        contextDataMap.put(ACCEPT_LANGUAGE, "en; ru;");
        contextDataMap.put(URL_HEADER, "api/v2/some-test-url");
        contextDataMap.put(WRONG_CUSTOM_HEADER, "tmp-value");
        contextDataMap.put(ALLOWED_HEADER, "custom_value");
        contextDataMap.put(X_REQUEST_ID_CONTEXT_NAME, "123");
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
