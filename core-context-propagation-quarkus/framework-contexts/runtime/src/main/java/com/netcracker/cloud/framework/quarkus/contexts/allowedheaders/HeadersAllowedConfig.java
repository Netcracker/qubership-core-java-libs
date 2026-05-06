package com.netcracker.cloud.framework.quarkus.contexts.allowedheaders;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

import java.util.Optional;

@ConfigMapping(prefix = "quarkus")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface HeadersAllowedConfig {
    /**
     * Allowed headers to propagate in contexts
     */
    @WithName("headers.allowed")
    Optional<String> allowedHeaders();

    /**
     * Blocked headers for context propagation. X-Channel-Request-Id is blocked by default.
     */
    @WithName("headers.blocked")
    Optional<String> blockedHeaders();
}
