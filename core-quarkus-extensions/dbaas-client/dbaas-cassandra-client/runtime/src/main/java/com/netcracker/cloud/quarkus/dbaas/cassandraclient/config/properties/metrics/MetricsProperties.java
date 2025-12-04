package com.netcracker.cloud.quarkus.dbaas.cassandraclient.config.properties.metrics;

import java.util.Optional;

public interface MetricsProperties {
    /**
     * Whether to enable metrics.
     */
    Optional<Boolean> enabled();

    /**
     * The session-level metrics configuration.
     */
    SessionProperties session();

    /**
     * The node-level metrics configuration.
     */
    NodeProperties node();
}
