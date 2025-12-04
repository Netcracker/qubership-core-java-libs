package com.netcracker.cloud.quarkus.dbaas.cassandraclient.config.properties.metrics;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public interface NodeProperties {

    /**
     * List of the enabled node-level metrics.
     */
    Optional<List<String>> enabled();

    /**
     * Extra configuration for 'cql-messages' metric.
     */
    MetricParameters cqlMessages();

    /**
     * Extra configuration for 'graph-messages' metric.
     */
    MetricParameters graphMessages();

    /**
     * The time after which the node level metrics will be evicted.
     */
    Optional<Duration> expireAfter();
}
