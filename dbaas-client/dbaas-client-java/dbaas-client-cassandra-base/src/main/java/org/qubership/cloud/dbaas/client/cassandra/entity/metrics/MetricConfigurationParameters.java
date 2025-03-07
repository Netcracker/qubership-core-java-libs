package org.qubership.cloud.dbaas.client.cassandra.entity.metrics;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Duration;
import java.util.List;

@Data
@AllArgsConstructor
public class MetricConfigurationParameters {
    private Duration highestLatency;
    private Duration lowestLatency;
    private int significantDigits;
    private Duration refreshInterval;
    private List<Duration> slo;
}
