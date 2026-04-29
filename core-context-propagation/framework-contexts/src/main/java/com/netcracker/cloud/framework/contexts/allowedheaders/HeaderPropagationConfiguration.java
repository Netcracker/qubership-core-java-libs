package com.netcracker.cloud.framework.contexts.allowedheaders;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class HeaderPropagationConfiguration {
    public static final String HEADERS_BLOCKED_PROPERTY = "headers.blocked";
    public static final String HEADERS_BLOCKED_ENV = "HEADERS_BLOCKED";
    public static final String DEFAULT_BLOCKED_HEADER = "X-Channel-Request-Id";
    public static final String NON_BLOCKABLE_HEADER = "X-Request-Id";

    private static volatile List<String> cachedBlockedHeaders = null;
    private static volatile Set<String> cachedBlockedHeadersLowerSet = null;

    private HeaderPropagationConfiguration() {
    }

    public static List<String> blockedHeaders() {
        if (cachedBlockedHeaders == null) {
            synchronized (HeaderPropagationConfiguration.class) {
                if (cachedBlockedHeaders == null) {
                    cachedBlockedHeaders = readBlockedHeaders();
                    cachedBlockedHeadersLowerSet = toLowerCaseSet(cachedBlockedHeaders);
                }
            }
        }
        return cachedBlockedHeaders;
    }

    public static void resetCache() {
        cachedBlockedHeaders = null;
        cachedBlockedHeadersLowerSet = null;
    }

    private static List<String> readBlockedHeaders() {
        boolean propertySpecified = System.getProperties().containsKey(HEADERS_BLOCKED_PROPERTY);
        String envValue = System.getenv(HEADERS_BLOCKED_ENV);
        boolean envSpecified = envValue != null;

        String blockedHeaders = propertySpecified
                ? System.getProperty(HEADERS_BLOCKED_PROPERTY)
                : envValue;

        boolean anySourceSpecified = propertySpecified || envSpecified;

        if (blockedHeaders == null || blockedHeaders.isBlank()) {
            return anySourceSpecified ? Collections.emptyList() : List.of(DEFAULT_BLOCKED_HEADER);
        }

        List<String> configuredBlockedHeaders = Arrays.stream(blockedHeaders.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .filter(s -> !s.equalsIgnoreCase(NON_BLOCKABLE_HEADER))
                .toList();

        // Keep default behavior: X-Channel-Request-Id remains blocked unless user explicitly sets empty list.
        return configuredBlockedHeaders.isEmpty() ? List.of(DEFAULT_BLOCKED_HEADER) : configuredBlockedHeaders;
    }

    public static boolean isBlacklisted(String headerName) {
        if (headerName == null || headerName.isBlank()) {
            return false;
        }
        blockedHeaders();
        return cachedBlockedHeadersLowerSet.contains(headerName.toLowerCase(Locale.ROOT));
    }

    private static Set<String> toLowerCaseSet(List<String> headers) {
        if (headers == null || headers.isEmpty()) {
            return Collections.emptySet();
        }
        return headers.stream()
                .filter(h -> h != null && !h.isBlank())
                .map(h -> h.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }
}
