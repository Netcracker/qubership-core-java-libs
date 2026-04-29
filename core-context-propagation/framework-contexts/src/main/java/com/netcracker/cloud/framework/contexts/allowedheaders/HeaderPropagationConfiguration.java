package com.netcracker.cloud.framework.contexts.allowedheaders;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class HeaderPropagationConfiguration {
    public static final String HEADERS_BLOCKED_PROPERTY = "headers.blocked";
    public static final String HEADERS_BLOCKED_ENV = "headers_blocked";
    public static final String DEFAULT_BLOCKED_HEADER = "X-Channel-Request-Id";

    private HeaderPropagationConfiguration() {
    }

    public static List<String> blockedHeaders() {
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

        return Arrays.stream(blockedHeaders.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public static boolean isExplicitlyBlacklisted(String headerName) {
        if (headerName == null || headerName.isBlank()) {
            return false;
        }

        for (String blockedHeader : blockedHeaders()) {
            if (blockedHeader.equalsIgnoreCase(headerName)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isBlacklisted(String headerName) {
        if (headerName == null || headerName.isBlank()) {
            return false;
        }

        return isExplicitlyBlacklisted(headerName);
    }
}
