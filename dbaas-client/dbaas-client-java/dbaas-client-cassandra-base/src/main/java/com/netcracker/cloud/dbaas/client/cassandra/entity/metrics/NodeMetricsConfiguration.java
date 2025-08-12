package com.netcracker.cloud.dbaas.client.cassandra.entity.metrics;

import lombok.Data;

import java.time.Duration;

@Data
public class NodeMetricsConfiguration extends CommonMetricsConfiguration {
    private MetricConfigurationParameters cqlMessages = new MetricConfigurationParameters(Duration.ofSeconds(3), Duration.ofMillis(1), 3, Duration.ofMinutes(5), null);
    private MetricConfigurationParameters graphMessages = new MetricConfigurationParameters(Duration.ofSeconds(3), Duration.ofMillis(1), 3, Duration.ofMinutes(5), null);
    private Duration expireAfter = Duration.ofHours(1);
}
