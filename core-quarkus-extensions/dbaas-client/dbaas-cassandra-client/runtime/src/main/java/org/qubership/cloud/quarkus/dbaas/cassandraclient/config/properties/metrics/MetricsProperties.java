package org.qubership.cloud.quarkus.dbaas.cassandraclient.config.properties.metrics;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Optional;

@ConfigGroup
@Getter
@Accessors(fluent = true)
public class MetricsProperties {
    /**
     * Whether to enable metrics.
     */
    @ConfigItem
    Optional<Boolean> enabled;

    /**
     * The session-level metrics configuration.
     */
    @ConfigItem
    SessionProperties session;

    /**
     * The node-level metrics configuration.
     */
    @ConfigItem
    NodeProperties node;
}
