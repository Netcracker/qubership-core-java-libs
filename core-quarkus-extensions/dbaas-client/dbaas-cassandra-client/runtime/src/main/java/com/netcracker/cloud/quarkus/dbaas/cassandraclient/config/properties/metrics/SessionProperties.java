package com.netcracker.cloud.quarkus.dbaas.cassandraclient.config.properties.metrics;

import java.util.List;
import java.util.Optional;

public interface SessionProperties {

    /**
     * List of the enabled session-level metrics.
     */
    Optional<List<String>> enabled();

    /**
     * Extra configuration for the 'cql-requests' metric.
     */
    MetricParameters cqlRequests();

    /**
     * Extra configuration for the 'throttling.delay' metric.
     */
    @io.smallrye.config.WithName("throttling.delay")
    MetricParameters throttling();

    /**
     * Extra configuration for 'continuous-cql-requests' metric.
     */
    MetricParameters continuousCqlRequests();

    /**
     * Extra configuration for 'graph-requests' metric.
     */
    MetricParameters graphRequests();
}
