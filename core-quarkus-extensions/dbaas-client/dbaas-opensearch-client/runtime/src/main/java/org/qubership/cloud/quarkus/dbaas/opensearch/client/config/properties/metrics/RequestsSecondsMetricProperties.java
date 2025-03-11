package org.qubership.cloud.quarkus.dbaas.opensearch.client.config.properties.metrics;

import org.qubership.cloud.dbaas.client.opensearch.entity.metrics.OpensearchClientRequestsSecondsMetricType;
import org.qubership.cloud.dbaas.client.opensearch.entity.metrics.OpensearchClientRequestsSecondsMetricsProperties;
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
public class RequestsSecondsMetricProperties {
    /**
     * Enabling configuring 'requests-seconds' metric for DBaaS Opensearch client.
     * Default value: true.
     */
    @ConfigItem(name = "enabled")
    Optional<Boolean> enabled;

    /**
     * Property to configure type of 'requests-seconds' metric. Possible values: SUMMARY or HISTOGRAM.
     * Default value: SUMMARY.
     */
    @ConfigItem(name = "type")
    Optional<OpensearchClientRequestsSecondsMetricType> type;

    /**
     * Minimum expected value of 'requests-seconds' metric.
     */
    @ConfigItem(name = "minimum-expected-value")
    Optional<Duration> minimumExpectedValue;

    /**
     * Maximum expected value of 'requests-seconds' metric.
     */
    @ConfigItem(name = "maximum-expected-value")
    Optional<Duration> maximumExpectedValue;

    /**
     * List of Double values configuring quantiles for 'requests-seconds' metric with type SUMMARY.
     * Default value: empty list.
     */
    @ConfigItem(name = "quantiles")
    Optional<List<Double>> quantiles;

    /**
     * Property to configure 'requests-seconds' metric with type SUMMARY to act like histogram (it is not histogram with buckets).
     * Default value: false.
     */
    @ConfigItem(name = "quantile-histogram")
    Optional<Boolean> quantileHistogram;

    /**
     * List of Double values configuring histogram buckets for 'requests-seconds' metric with type HISTOGRAM.
     * Default value: empty list.
     */
    @ConfigItem(name = "histogram-buckets")
    Optional<List<Duration>> histogramBuckets;

    public OpensearchClientRequestsSecondsMetricsProperties toOpensearchClientRequestsSecondsMetricsProperties() {
        var metricsProperties = new OpensearchClientRequestsSecondsMetricsProperties();

        metricsProperties.setEnabled(enabled.orElse(Boolean.TRUE));
        metricsProperties.setType(type.orElse(OpensearchClientRequestsSecondsMetricType.SUMMARY));
        metricsProperties.setMinimumExpectedValue(minimumExpectedValue.orElse(Duration.ofMillis(1)));
        metricsProperties.setMaximumExpectedValue(maximumExpectedValue.orElse(Duration.ofSeconds(30)));
        metricsProperties.setQuantiles(quantiles.orElse(List.of()));
        metricsProperties.setQuantileHistogram(quantileHistogram.orElse(Boolean.FALSE));
        metricsProperties.setHistogramBuckets(histogramBuckets.orElse(List.of()));

        return metricsProperties;
    }
}
