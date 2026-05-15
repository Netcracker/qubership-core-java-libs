package com.netcracker.cloud.framework.contexts.allowedheaders;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public final class HeaderPropagationConfiguration {
    public static final String HEADERS_BLOCKED_PROPERTY = "headers.blocked";
    public static final String HEADERS_BLOCKED_ENV = "HEADERS_BLOCKED";
    public static final List<String> DEFAULT_BLOCKED_HEADERS = 
    List.of("X-Channel-Request-Id");

    public static final List<String> NON_BLOCKABLE_HEADERS = 
        List.of("X-Request-Id");

    private static final AtomicReference<CachedHeaders> cachedHeaders = new AtomicReference<>(null);

    private static final class CachedHeaders {
        final List<String> list;
        final Set<String> lowerSet;

        CachedHeaders(List<String> list) {
            this.list = list;
            this.lowerSet = list.stream()
                    .filter(h -> h != null && !h.isBlank())
                    .map(h -> h.toLowerCase(Locale.ROOT))
                    .collect(Collectors.toUnmodifiableSet());
        }
    }

    private HeaderPropagationConfiguration() {
    }

    private static CachedHeaders getOrInit() {
        CachedHeaders local = cachedHeaders.get();
        if (local == null) {
            synchronized (HeaderPropagationConfiguration.class) {
                local = cachedHeaders.get();
                if (local == null) {
                    local = new CachedHeaders(readBlockedHeaders());
                    cachedHeaders.set(local);
                }
            }
        }
        return local;
    }

    public static List<String> blockedHeaders() {
        return getOrInit().list;
    }

    public static void resetCache() {
        cachedHeaders.set(null);
    }

    public static boolean isBlacklisted(String headerName) {
        if (headerName == null || headerName.isBlank()) {
            return false;
        }
        return getOrInit().lowerSet.contains(headerName.toLowerCase(Locale.ROOT));
    }

    private static List<String> readBlockedHeaders() {
        boolean propertySpecified = System.getProperties().containsKey(HEADERS_BLOCKED_PROPERTY);
        String envValue = System.getenv(HEADERS_BLOCKED_ENV);
        boolean envSpecified = envValue != null;

        // No source set at all → fall back to the built-in default.
        if (!propertySpecified && !envSpecified) {
            return DEFAULT_BLOCKED_HEADERS;
        }

        // Property wins over env when both are set.
        String raw = propertySpecified
                ? System.getProperty(HEADERS_BLOCKED_PROPERTY)
                : envValue;

        // Source is set but empty/blank → explicit "erase the default".
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }

        // Source is set with a value → parse, trim, drop non-blockable entries.
        // If everything filters out, we still respect the user's explicit override
        // and return an empty list — we do NOT silently restore the default.
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .filter(s -> NON_BLOCKABLE_HEADERS.stream()
                        .noneMatch(s::equalsIgnoreCase))
                .toList();
    }
}
