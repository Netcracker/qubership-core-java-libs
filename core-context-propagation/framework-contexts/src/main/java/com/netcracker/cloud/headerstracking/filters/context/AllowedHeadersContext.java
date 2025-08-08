package org.qubership.cloud.headerstracking.filters.context;

import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.framework.contexts.allowedheaders.AllowedHeadersContextObject;

import java.util.Map;

import static org.qubership.cloud.framework.contexts.allowedheaders.AllowedHeadersProvider.ALLOWED_HEADER;

public class AllowedHeadersContext {

    public static Map<String, String> getHeaders() {
        AllowedHeadersContextObject allowedHeadersContextObject = ContextManager.get(ALLOWED_HEADER);
        return allowedHeadersContextObject.getHeaders();
    }

    public static void set(Map<String, String> headers) {
        AllowedHeadersContextObject allowedHeadersContextObject = new AllowedHeadersContextObject(headers);
        ContextManager.set(ALLOWED_HEADER, allowedHeadersContextObject);
    }

    public static void clear() {
        ContextManager.clear(ALLOWED_HEADER);
    }
}