package com.netcracker.cloud.dbaas.client.opensearch.metrics;

import org.qubership.cloud.dbaas.client.exceptions.MetricsRegistrationException;
import org.qubership.cloud.dbaas.client.metrics.DatabaseMetricProperties;
import org.qubership.cloud.dbaas.client.metrics.MetricsProvider;
import org.qubership.cloud.dbaas.client.opensearch.entity.DbaasOpensearchMetricsProperties;
import org.qubership.cloud.dbaas.client.opensearch.entity.OpensearchDBType;
import org.qubership.cloud.dbaas.client.opensearch.entity.OpensearchIndex;
import org.qubership.cloud.dbaas.client.opensearch.entity.metrics.OpensearchClientRequestsSecondsMetricType;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.httpcomponents.hc5.ObservationExecChainHandler;
import io.micrometer.observation.ObservationRegistry;
import lombok.AllArgsConstructor;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;

import java.time.Duration;
import java.util.Optional;

@AllArgsConstructor
public class OpensearchMetricsProvider implements MetricsProvider<OpensearchIndex> {

    public static final String REQUESTS_SECONDS_METRIC_NAME = "opensearch_client_requests_seconds";
    public static final String REQUESTS_SECONDS_METRIC_DESCRIPTION = "OpenSearch client requests time in seconds";

    public static final String HTTP_CLIENT_BUILDER = "http_client_builder";
    public static final String MICROMETER_EXEC_INTERCEPTOR_NAME = "micrometer";

    private final MeterRegistry meterRegistry;
    private final DbaasOpensearchMetricsProperties opensearchMetricsProperties;

    @Override
    public Class getSupportedDatabaseType() {
        return OpensearchDBType.INSTANCE.getDatabaseClass();
    }

    @Override
    public void registerMetrics(DatabaseMetricProperties metricProperties) throws MetricsRegistrationException {
        if (Boolean.TRUE.equals(opensearchMetricsProperties.getEnabled())) {
            var requestsSecondsMetricsProperties = opensearchMetricsProperties.getRequestsSeconds();

            if (Boolean.TRUE.equals(requestsSecondsMetricsProperties.getEnabled())) {
                createRequestSecondsMetric(metricProperties);
            }
        }
    }

    protected void createRequestSecondsMetric(DatabaseMetricProperties metricProperties) throws MetricsRegistrationException {
        var httpClientBuilder = (HttpAsyncClientBuilder) metricProperties.getExtraParameters()
            .get(HTTP_CLIENT_BUILDER);

        if (httpClientBuilder != null) {
            var observationRegistry = ObservationRegistry.create();

            observationRegistry.observationConfig().observationHandler(
                new OpensearchClientRequestsSecondsObservationHandler(
                    metricProperties.getMetricTags(), meterRegistry, getRequestsSecondsBaseMetricBuilder()
                )
            );

            var observationExecChainHandler = new ObservationExecChainHandler(observationRegistry);

            httpClientBuilder.addExecInterceptorLast(MICROMETER_EXEC_INTERCEPTOR_NAME, observationExecChainHandler);
        } else {
            throw new MetricsRegistrationException(String.format(
                "DatabaseMetricProperties instance does not contain value for extra parameter %s in order to create %s metric",
                HTTP_CLIENT_BUILDER, REQUESTS_SECONDS_METRIC_NAME
            ));
        }
    }

    protected Timer.Builder getRequestsSecondsBaseMetricBuilder() throws MetricsRegistrationException {
        var requestsSecondsMetricsProperties = opensearchMetricsProperties.getRequestsSeconds();

        var metricBuilder = Timer.builder(REQUESTS_SECONDS_METRIC_NAME)
            .description(REQUESTS_SECONDS_METRIC_DESCRIPTION)
            .minimumExpectedValue(requestsSecondsMetricsProperties.getMinimumExpectedValue())
            .maximumExpectedValue(requestsSecondsMetricsProperties.getMaximumExpectedValue());

        var metricType = requestsSecondsMetricsProperties.getType();

        if (OpensearchClientRequestsSecondsMetricType.SUMMARY.equals(metricType)) {

            var quantiles = requestsSecondsMetricsProperties.getQuantiles().stream()
                .mapToDouble(Double::doubleValue)
                .toArray();

            metricBuilder.publishPercentiles(quantiles)
                .publishPercentileHistogram(requestsSecondsMetricsProperties.getQuantileHistogram());

        } else if (OpensearchClientRequestsSecondsMetricType.HISTOGRAM.equals(metricType)) {

            metricBuilder.serviceLevelObjectives(
                requestsSecondsMetricsProperties.getHistogramBuckets().toArray(Duration[]::new)
            );
        } else {
            var metricTypeName = Optional.ofNullable(metricType)
                .map(Enum::name)
                .orElse(null);

            throw new MetricsRegistrationException(String.format(
                "Unsupported %s type for metric %s", metricTypeName, REQUESTS_SECONDS_METRIC_NAME
            ));
        }

        return metricBuilder;
    }
}
