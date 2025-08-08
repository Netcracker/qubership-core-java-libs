package org.qubership.cloud.framework.contexts.allowedheaders;

import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.context.propagation.core.contextdata.IncomingContextData;
import org.qubership.cloud.context.propagation.core.contextdata.OutgoingContextData;
import org.qubership.cloud.context.propagation.core.contexts.DefaultValueAwareContext;
import org.qubership.cloud.context.propagation.core.contexts.ResponsePropagatableContext;
import org.qubership.cloud.context.propagation.core.contexts.SerializableContext;
import org.qubership.cloud.context.propagation.core.contexts.SerializableDataContext;
import org.qubership.cloud.context.propagation.core.contexts.common.RequestContextObject;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class AllowedHeadersContextObject implements SerializableContext,
        DefaultValueAwareContext<Map<String, String>>, ResponsePropagatableContext, SerializableDataContext {

    private List<String> allowedHeaders = Collections.emptyList();

    private Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    public AllowedHeadersContextObject(@Nullable IncomingContextData contextData, List<String> allowedHeaders) {
        this.allowedHeaders = allowedHeaders;

        if (contextData != null) {
            for (String headerName : this.allowedHeaders) {
                if (contextData.get(headerName) != null) {
                    headers.put(headerName, (String) contextData.get(headerName));
                }
            }
        } else {
            this.headers = getDefault();
        }
    }

    public AllowedHeadersContextObject(Map<String, String> headers){
        this.headers = headers;
    }

    @Override
    public void serialize(OutgoingContextData outgoingContextData) {
        for (String headerName : headers.keySet()) {
            outgoingContextData.set(headerName, headers.get(headerName));
        }
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    @Override
    public Map<String, String> getDefault() {
        Map<String, List<String>> headersFromContext = ((RequestContextObject) ContextManager.get("request")).getHttpHeaders();
        Map<String, String> result = new HashMap<>();
        for (String headerName : headersFromContext.keySet()) {
            if (allowedHeaders.contains(headerName)) {
                result.put(headerName, headersFromContext.get(headerName).get(0));
            }
        }
        return result;
    }

    @Override
    public void propagate(OutgoingContextData outgoingContextData) {
        for (String headerName : headers.keySet()) {
            outgoingContextData.set(headerName, headers.get(headerName));
        }
    }

    @Override
    public Map<String, Object> getSerializableContextData() {
        return new HashMap<>(headers);
    }
}
