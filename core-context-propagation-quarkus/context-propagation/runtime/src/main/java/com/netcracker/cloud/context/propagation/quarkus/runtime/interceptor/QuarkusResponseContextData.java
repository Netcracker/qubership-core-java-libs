package com.netcracker.cloud.context.propagation.quarkus.runtime.interceptor;

import org.qubership.cloud.context.propagation.core.contextdata.OutgoingContextData;
import jakarta.ws.rs.core.MultivaluedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class QuarkusResponseContextData implements OutgoingContextData {

    private static final Logger log = LoggerFactory.getLogger(QuarkusResponseContextData.class);

    private Map<String, Object> responseHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    public Map<String, Object> getResponseHeaders() {
        return responseHeaders;
    }

    @Override
    public void set(String name, Object values) {
        responseHeaders.put(name, values);
    }

    public void addHeadersToMap(MultivaluedMap<String, Object> headersMap) {
        for (Map.Entry<String, Object> entry : responseHeaders.entrySet()) {
            if (headersMap != null && !headersMap.containsKey(entry.getKey())) {
                log.trace("Add header={} with value={} from context", entry.getKey(), entry.getValue());
                if (entry.getValue() instanceof List) {
                    headersMap.put(entry.getKey(), (List) entry.getValue());
                } else {
                    headersMap.putSingle(entry.getKey(), entry.getValue());
                }
            }
        }
    }
}
