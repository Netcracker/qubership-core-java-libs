package com.netcracker.cloud.framework.contexts.xchannelrequestid;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Computes the effective set of restricted headers — i.e. headers that are NOT propagated to
 * outgoing requests by default.
 *
 * <h2>Model</h2>
 * <ul>
 *     <li>The framework owns a hard-coded list of restricted headers
 *         ({@link #RESTRICTED_HEADERS}). It cannot be changed from configuration. By default it
 *         restricts {@code X-Channel-Request-Id}.</li>
 *     <li>A user-facing system property {@value #ENABLE_OPTIONAL_PROPERTY} carries a
 *         comma-separated list of restricted header names that should be enabled for propagation.
 *         Names that are not in the restricted list have no effect.</li>
 * </ul>
 *
 */
public final class HeaderPropagationConfiguration {

    /** System property carrying header names to enable for propagation, comma-separated. */
    public static final String ENABLE_OPTIONAL_PROPERTY = "context.propagation.headers.enable.optional";

    /**
     * Hard-coded list of restricted headers — contexts that the framework refuses to propagate
     * unless explicitly enabled via {@link #ENABLE_OPTIONAL_PROPERTY}.
     */
    public static final List<String> RESTRICTED_HEADERS = List.of(XChannelRequestIdContextProvider.X_CHANNEL_REQUEST_ID_CONTEXT_NAME);

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
                    local = new CachedHeaders(computeEffectiveRestricted());
                    cachedHeaders.set(local);
                }
            }
        }
        return local;
    }

    /**
     * @return effective list of headers that are restricted from propagation right now
     *         (the hard-coded {@link #RESTRICTED_HEADERS} minus any enabled via
     *         {@link #ENABLE_OPTIONAL_PROPERTY}).
     */
    public static List<String> restrictedHeaders() {
        return getOrInit().list;
    }

    public static void resetCache() {
        cachedHeaders.set(null);
    }

    /**
     * @return {@code true} iff the given header is currently restricted from propagation.
     */
    public static boolean isRestricted(String headerName) {
        if (headerName == null || headerName.isBlank()) {
            return false;
        }
        return getOrInit().lowerSet.contains(headerName.toLowerCase(Locale.ROOT));
    }

    private static List<String> computeEffectiveRestricted() {
        Set<String> enabled = readEnabledOptional();
        if (enabled.isEmpty()) {
            return RESTRICTED_HEADERS;
        }
        return RESTRICTED_HEADERS.stream()
                .filter(h -> !enabled.contains(h.toLowerCase(Locale.ROOT)))
                .toList();
    }

    private static Set<String> readEnabledOptional() {
        String raw = System.getProperty(ENABLE_OPTIONAL_PROPERTY);
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
