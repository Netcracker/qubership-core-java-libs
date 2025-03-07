package org.qubership.cloud.dbaas.client.cassandra.entity.metrics;

import lombok.Data;

import java.time.Duration;

@Data
public class SessionMetricsConfiguration extends CommonMetricsConfiguration {
    private MetricConfigurationParameters cqlRequests = new MetricConfigurationParameters(Duration.ofSeconds(3), Duration.ofMillis(1), 3, Duration.ofMinutes(5), null);
    private ThrottlingDelayMetricConfiguration throttling = new ThrottlingDelayMetricConfiguration();
    private MetricConfigurationParameters continuousCqlRequests = new MetricConfigurationParameters(Duration.ofSeconds(120), Duration.ofMillis(10), 3, Duration.ofMinutes(5), null);
    private MetricConfigurationParameters graphRequests = new MetricConfigurationParameters(Duration.ofSeconds(12), Duration.ofMillis(1), 3, Duration.ofMinutes(5), null);
}
