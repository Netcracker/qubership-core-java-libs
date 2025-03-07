package org.qubership.cloud.dbaas.client.cassandra.entity.metrics;

import lombok.Data;

import java.time.Duration;

@Data
public class ThrottlingDelayMetricConfiguration {
    private MetricConfigurationParameters delay = new MetricConfigurationParameters(Duration.ofSeconds(3), Duration.ofMillis(1), 3, Duration.ofMinutes(5), null);
}
