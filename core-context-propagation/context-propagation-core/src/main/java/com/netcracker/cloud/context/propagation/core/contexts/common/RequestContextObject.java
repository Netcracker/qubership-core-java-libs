package com.netcracker.cloud.context.propagation.core.contexts.common;

import org.qubership.cloud.context.propagation.core.contextdata.IncomingContextData;
import org.qubership.cloud.context.propagation.core.contexts.DefaultValueAwareContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class RequestContextObject implements DefaultValueAwareContext<Map<String, List<String>>> {
    private final Map<String, List<String>> headers;

    public RequestContextObject(@Nullable IncomingContextData incomingContextData) {
        this(getHeaders(incomingContextData));
    }

    public RequestContextObject(Map<String, ?> headers) {
        if (headers != null) {
            Map<String, List<String>> lowercaseHeaders = new HashMap<>();
            headers.entrySet().stream()
                    .filter(entry -> Objects.nonNull(entry.getKey()) && Objects.nonNull(entry.getValue()))
                    .forEach(headerEntry -> storeHeaderPair(headerEntry, lowercaseHeaders));
            this.headers = Collections.unmodifiableMap(lowercaseHeaders);
        } else {
            this.headers = getDefault();
        }
    }

    protected static Map<String, List<?>> getHeaders(@Nullable IncomingContextData incomingContextData) {
        Map<String, List<?>> headers = null;
        if (incomingContextData != null) {
            headers = incomingContextData.getAll();
        }
        return headers;
    }

    @NotNull
    public List<String> getHttpHeader(@NotNull String header) {
        if (headers == null) {
            return Collections.emptyList();
        }
        List<String> headerList = headers.get(header.toLowerCase());
        return headerList != null ? headerList : Collections.emptyList();
    }

    @NotNull
    public Map<String, List<String>> getHttpHeaders() {
        if (headers == null) {
            return Collections.emptyMap();
        }
        return headers;
    }

    @Nullable
    public String getFirst(String headerName) {
        List<String> header = getHttpHeader(headerName);
        if (!header.isEmpty()) {
            return header.get(0);
        } else {
            return null;
        }
    }

    @Override
    public Map<String, List<String>> getDefault() {
        return Collections.emptyMap();
    }

    private void storeHeaderPair(Map.Entry<String, ?> headerEntry, Map<String, List<String>> storeSource) {
        String headerKey = headerEntry.getKey();
        Object value = headerEntry.getValue();
        List<String> headerValue;
        if (value instanceof List) {
            headerValue = ((List<?>) value).stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());
        } else {
            headerValue = Collections.singletonList(value.toString());
        }
        storeSource.put(headerKey.toLowerCase(), headerValue);
    }
}

