package com.netcracker.cloud.framework.quarkus.contexts.allowedheaders;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

import java.util.Optional;


@ConfigRoot(name = "", phase = ConfigPhase.RUN_TIME)
public class HeadersAllowedConfig {
    /**
     * Allowed headers to propagate in contexts
     */
    @ConfigItem(name = "headers.allowed")
    public Optional<String> allowedHeaders = Optional.empty();
}
