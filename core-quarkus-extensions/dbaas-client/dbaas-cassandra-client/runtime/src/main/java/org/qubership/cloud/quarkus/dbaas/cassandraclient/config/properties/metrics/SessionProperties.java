package org.qubership.cloud.quarkus.dbaas.cassandraclient.config.properties.metrics;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Optional;

@ConfigGroup
@Getter
@Accessors(fluent = true)
public class SessionProperties {
    /**
     * List of the enabled session-level metrics.
     */
    @ConfigItem
    Optional<List<String>> enabled;

    /**
     * Extra configuration for the 'cql-requests' metric.
     */
    @ConfigItem
    MetricParameters cqlRequests;

    /**
     * Extra configuration for the 'throttling.delay' metric.
     */
    @ConfigItem(name = "throttling.delay")
    MetricParameters throttling;

    /**
     * Extra configuration for 'continuous-cql-requests' metric.
     */
    @ConfigItem
    MetricParameters continuousCqlRequests;

    /**
     * Extra configuration for 'graph-requests' metric.
     */
    @ConfigItem
    MetricParameters graphRequests;
}
