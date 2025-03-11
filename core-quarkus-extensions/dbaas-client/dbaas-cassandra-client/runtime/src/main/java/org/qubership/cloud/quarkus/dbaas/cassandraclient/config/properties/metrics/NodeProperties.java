package org.qubership.cloud.quarkus.dbaas.cassandraclient.config.properties.metrics;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@ConfigGroup
@Getter
@Accessors(fluent = true)
public class NodeProperties {
    /**
     * List of the enabled node-level metrics.
     */
    @ConfigItem
    Optional<List<String>> enabled;

    /**
     * Extra configuration for 'cql-messages' metric.
     */
    @ConfigItem
    MetricParameters cqlMessages;

    /**
     * Extra configuration for 'graph-messages' metric.
     */
    @ConfigItem
    MetricParameters graphMessages;

    /**
     * The time after which the node level metrics will be evicted.
     */
    @ConfigItem
    Optional<Duration> expireAfter;
}
