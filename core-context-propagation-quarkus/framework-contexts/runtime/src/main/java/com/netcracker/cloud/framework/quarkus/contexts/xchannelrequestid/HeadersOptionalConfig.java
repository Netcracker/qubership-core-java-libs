package com.netcracker.cloud.framework.quarkus.contexts.xchannelrequestid;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

import java.util.List;
import java.util.Optional;

/**
 * Configuration for headers that are restricted from propagation by default and may be
 * optionally enabled via {@code quarkus.context.propagation.headers.enable.optional}.
 *
 * <p>The framework owns a hard-coded list of restricted headers (currently {@code X-Channel-Request-Id})
 * that are not propagated to outgoing requests by default. This configuration lets users opt in
 * to propagating one or more of those restricted headers by listing them, comma-separated, in the
 * {@code quarkus.context.propagation.headers.enable.optional} property.</p>
 *
 */
@ConfigMapping(prefix = "quarkus.context.propagation.headers")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface HeadersOptionalConfig {

    /**
     * Comma-separated list of restricted headers to enable for propagation. Header names that
     * are not part of the framework's restricted list are ignored.
     *
     * <p>When the value is absent or empty, the restricted list applies in full.</p>
     */
    @WithName("enable.optional")
    Optional<List<String>> enableOptional();
}
