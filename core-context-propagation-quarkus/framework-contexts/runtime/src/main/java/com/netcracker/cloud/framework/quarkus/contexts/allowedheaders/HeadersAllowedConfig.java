package com.netcracker.cloud.framework.quarkus.contexts.allowedheaders;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

import java.util.Optional;

@ConfigMapping(prefix = "headers")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface HeadersAllowedConfig {
    /**
     * Allowed headers to propagate in contexts
     */
    @WithName("allowed")
    Optional<String> allowedHeaders();
}
