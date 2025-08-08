package org.qubership.cloud.dbaas.client.opensearch.entity.metrics;

import lombok.Data;

import java.time.Duration;
import java.util.List;

@Data
public class OpensearchClientRequestsSecondsMetricsProperties {

    private Boolean enabled = Boolean.TRUE;
    private OpensearchClientRequestsSecondsMetricType type = OpensearchClientRequestsSecondsMetricType.SUMMARY;
    private Duration minimumExpectedValue = Duration.ofMillis(1);
    private Duration maximumExpectedValue = Duration.ofSeconds(30);

    // Summary specific properties
    private List<Double> quantiles = List.of();
    private Boolean quantileHistogram = Boolean.FALSE;

    // Histogram specific properties
    private List<Duration> histogramBuckets = List.of();
}
