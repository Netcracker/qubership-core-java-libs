package com.netcracker.cloud.framework.contexts.allowedheaders;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Computes the effective set of headers that must not be propagated to outgoing requests.
 *
 * <h2>Model</h2>
 * <ul>
 *     <li>The framework owns an internal blocklist ({@link #INTERNAL_BLOCKED_HEADERS}). It is
 *         hard-coded and cannot be changed from configuration. By default it blocks
 *         {@code X-Channel-Request-Id}.</li>
 *     <li>A user-facing system property {@value #ALLOW_BLOCKED_PROPERTY} carries a
 *         comma-separated list of header names that should be exempted from the internal
 *         blocklist (i.e. allowed to propagate). Names that are not in the internal
 *         blocklist have no effect.</li>
 * </ul>
 *
 * <h2>How the property is supplied</h2>
 * The Quarkus extension copies {@code quarkus.context.propagation.allow-blocked-headers} into this system property
 * at {@code RUNTIME_INIT}; the Spring configuration does the same for the Spring-style
 * {@code context.propagation.allow-blocked-headers} property. In both cases standard env-to-property mapping
 * applies, so a container ENV variable like {@code CONTEXT_PROPAGATION_ALLOW_BLOCKED_HEADERS=X-Channel-Request-Id}
 * propagates all the way through to this class.
 */
public final class HeaderPropagationConfiguration {

    /** System property carrying exempted-from-blocklist header names, comma-separated. */
    public static final String ALLOW_BLOCKED_PROPERTY = "context.propagation.allow-blocked-headers";

    /**
     * Hard-coded internal blocklist of headers that the framework refuses to propagate
     * unless explicitly exempted via {@link #ALLOW_BLOCKED_PROPERTY}.
     */
    public static final List<String> INTERNAL_BLOCKED_HEADERS = List.of("X-Channel-Request-Id");

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
                    local = new CachedHeaders(computeEffectiveBlocklist());
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

    private static List<String> computeEffectiveBlocklist() {
        Set<String> exemptions = readExemptions();
        if (exemptions.isEmpty()) {
            return INTERNAL_BLOCKED_HEADERS;
        }
        return INTERNAL_BLOCKED_HEADERS.stream()
                .filter(h -> !exemptions.contains(h.toLowerCase(Locale.ROOT)))
                .toList();
    }

    private static Set<String> readExemptions() {
        String raw = System.getProperty(ALLOW_BLOCKED_PROPERTY);
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }
}
