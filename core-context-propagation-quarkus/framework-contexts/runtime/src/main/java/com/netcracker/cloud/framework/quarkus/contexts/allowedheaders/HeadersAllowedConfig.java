package com.netcracker.cloud.framework.quarkus.contexts.allowedheaders;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

import java.util.List;
import java.util.Optional;

@ConfigMapping(prefix = "quarkus")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface HeadersAllowedConfig {

    /**
     * Allowed headers to propagate in contexts.
     */
    @WithName("headers.allowed")
    Optional<String> allowedHeaders();

    /**
     * Headers that should be allowed to propagate even though they appear in the framework's
     * internal blocklist. The blocklist itself is hard-coded and cannot be changed from
     * configuration.
     *
     * <p>Semantics:
     * <ul>
     *     <li>Property not set or empty: the internal blocklist is applied as-is. Any header
     *         in the blocklist (for example {@code X-Channel-Request-Id}) is not propagated.</li>
     *     <li>Property contains a comma-separated list of header names: each listed name is
     *         removed from the effective blocklist and is allowed to propagate. Names that
     *         are not in the internal blocklist have no effect.</li>
     * </ul>
     *
     * <p>Example {@code application.yaml} with a container ENV indirection:
     * <pre>
     * quarkus:
     *   context:
     *     propagation:
     *       allow-blocked-headers: ${CONTEXT_PROPAGATION_ALLOW_BLOCKED_HEADERS:}
     * </pre>
     * The container then sets {@code CONTEXT_PROPAGATION_ALLOW_BLOCKED_HEADERS=X-Channel-Request-Id} when it needs
     * that header to be propagated.
     */
    @WithName("context.propagation.allow-blocked-headers")
    Optional<List<String>> allowedHeadersFromBlocklist();
}
