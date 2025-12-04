package com.netcracker.cloud.quarkus.dbaas.opensearch.client.config.properties.metrics;

import com.netcracker.cloud.dbaas.client.opensearch.entity.metrics.OpensearchClientRequestsSecondsMetricType;
import com.netcracker.cloud.dbaas.client.opensearch.entity.metrics.OpensearchClientRequestsSecondsMetricsProperties;
import io.smallrye.config.WithName;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public interface RequestsSecondsMetricProperties {

    /**
     * Enabling configuring 'requests-seconds' metric for DBaaS Opensearch client.
     * Default value: true.
     */
    @WithName("enabled")
    Optional<Boolean> enabled();

    /**
     * Property to configure type of 'requests-seconds' metric. Possible values: SUMMARY or HISTOGRAM.
     * Default value: SUMMARY.
     */
    @WithName("type")
    Optional<OpensearchClientRequestsSecondsMetricType> type();

    /**
     * Minimum expected value of 'requests-seconds' metric.
     */
    @WithName("minimum-expected-value")
    Optional<Duration> minimumExpectedValue();

    /**
     * Maximum expected value of 'requests-seconds' metric.
     */
    @WithName("maximum-expected-value")
    Optional<Duration> maximumExpectedValue();

    /**
     * List of Double values configuring quantiles for 'requests-seconds' metric with type SUMMARY.
     * Default value: empty list.
     */
    @WithName("quantiles")
    Optional<List<Double>> quantiles();

    /**
     * Property to configure 'requests-seconds' metric with type SUMMARY to act like histogram (it is not histogram with buckets).
     * Default value: false.
     */
    @WithName("quantile-histogram")
    Optional<Boolean> quantileHistogram();

    /**
     * List of Double values configuring histogram buckets for 'requests-seconds' metric with type HISTOGRAM.
     * Default value: empty list.
     */
    @WithName("histogram-buckets")
    Optional<List<Duration>> histogramBuckets();

    default OpensearchClientRequestsSecondsMetricsProperties toOpensearchClientRequestsSecondsMetricsProperties() {
        var metricsProperties = new OpensearchClientRequestsSecondsMetricsProperties();

        metricsProperties.setEnabled(enabled().orElse(Boolean.TRUE));
        metricsProperties.setType(type().orElse(OpensearchClientRequestsSecondsMetricType.SUMMARY));
        metricsProperties.setMinimumExpectedValue(minimumExpectedValue().orElse(Duration.ofMillis(1)));
        metricsProperties.setMaximumExpectedValue(maximumExpectedValue().orElse(Duration.ofSeconds(30)));
        metricsProperties.setQuantiles(quantiles().orElse(List.of()));
        metricsProperties.setQuantileHistogram(quantileHistogram().orElse(Boolean.FALSE));
        metricsProperties.setHistogramBuckets(histogramBuckets().orElse(List.of()));

        return metricsProperties;
    }
}
