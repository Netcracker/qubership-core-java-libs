package com.netcracker.cloud.contexts;

import org.qubership.cloud.context.propagation.core.contextdata.IncomingContextData;

import java.util.*;

import static java.util.stream.Collectors.toMap;
import static jakarta.ws.rs.core.HttpHeaders.ACCEPT_LANGUAGE;

public class IncomingContextDataFactory {

    public static IncomingContextData getRequestIncomingContextData() {
        return new IncomingContextDataImpl(requestData());
    }

    public static IncomingContextData getAcceptLanguageIncomingContextData() {
        return new IncomingContextDataImpl(acceptLanguageData());
    }

    public static IncomingContextData getAllowedHeadersIncomingContextData() {
        return new IncomingContextDataImpl(allowedHeadersData());
    }

    public static IncomingContextData getApiVersionIncomingContextData() {
        return new IncomingContextDataImpl(apiVersionData());
    }

    public static IncomingContextData getXRequestIdIncomingContextData() {
        return new IncomingContextDataImpl(xRequestIdData());
    }

    public static IncomingContextData getXVersionIncomingContextData() {
        return new IncomingContextDataImpl(xVersionData());
    }

    private static class IncomingContextDataImpl implements IncomingContextData {
        private Map<String, Object> contextData;

        public IncomingContextDataImpl(Map<String, Object> contextData) {
            this.contextData = contextData;
        }

        @Override
        public Object get(String name) {
            return contextData.get(name);
        }

        @Override
        public Map<String, List<?>> getAll() {
            return contextData.entrySet().stream().collect(toMap(Map.Entry::getKey, e -> {
                Object data = e.getValue();
                if (data instanceof List) {
                    return (List<?>) data;
                }
                return Collections.singletonList(data);
            }));
        }
    }

    private static Map<String, Object> requestData() {
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("Header-1", "Header-1-Value");
        requestData.put("Header-2", "Header-2-Value");
        return requestData;
    }

    private static Map<String, Object> acceptLanguageData() {
        Map<String, Object> requestData = new HashMap<>();
        requestData.put(ACCEPT_LANGUAGE, "ru; en");
        return requestData;
    }

    private static Map<String, Object> allowedHeadersData() {
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("my-header", "my-value");
        return requestData;
    }

    private static Map<String, Object> apiVersionData() {
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("cloud-core.context-propagation.url", "api/v2/tenant-manager");
        return requestData;
    }

    private static Map<String, Object> xRequestIdData() {
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("X-Request-Id", UUID.randomUUID().toString());
        return requestData;
    }

    private static Map<String, Object> xVersionData() {
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("X-Version", "2");
        return requestData;
    }
}
