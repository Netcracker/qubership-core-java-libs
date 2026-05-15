package com.netcracker.cloud.framework.quarkus.contexts.allowedheaders;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

import java.util.Optional;

@ConfigMapping(prefix = "quarkus")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface HeadersAllowedConfig {

    /**
     * Sentinel value used to distinguish "property not set" from "property explicitly set to empty".
     * Starts with a NUL character so users cannot accidentally type it.
     */
    String UNSET = "\0__unset__";

    /**
     * Allowed headers to propagate in contexts.
     */
    @WithName("headers.allowed")
    Optional<String> allowedHeaders();

    /**
     * Blocked headers for context propagation. X-Channel-Request-Id is blocked by default.
     * <p>
     * Three distinct states are supported:
     * <ul>
     *     <li>property not set — value equals {@link #UNSET}; the default blocked list applies.</li>
     *     <li>property set to empty — value equals "" (empty string); the default is erased.</li>
     *     <li>property set to a value — value is the raw configured string.</li>
     * </ul>
     * The custom {@link RawStringConverter} is required so that an empty value is preserved
     * instead of being collapsed to {@code null} by SmallRye's default String converter.
     */
    @WithName("headers.blocked")
    @WithConverter(RawStringConverter.class)
    @WithDefault(UNSET)
    String blockedHeaders();

    /**
     * @return {@code true} if the user explicitly configured {@code quarkus.headers.blocked}
     *         (including to an empty value); {@code false} if the property was not set at all.
     */
    default boolean isBlockedHeadersSet() {
        return !UNSET.equals(blockedHeaders());
    }
}
