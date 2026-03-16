package com.netcracker.cloud.quarkus.dbaas.opensearch.client.config.properties.metrics;

import com.netcracker.cloud.dbaas.client.opensearch.entity.DbaasOpensearchMetricsProperties;
import io.smallrye.config.WithName;

import java.util.Optional;

public interface OpensearchMetricsProperties {

    /**
     * Enabling configuring metrics for DBaaS Opensearch client. Default value: true.
     */
    @WithName("enabled")
    Optional<Boolean> enabled();

    /**
     * Properties for configuring {@link com.netcracker.cloud.dbaas.client.opensearch.metrics.OpensearchMetricsProvider#REQUESTS_SECONDS_METRIC_NAME} metric.
     */
    @WithName("requests-seconds")
    RequestsSecondsMetricProperties requestsSeconds();

    default DbaasOpensearchMetricsProperties toDbaasOpensearchMetricsProperties() {
        var metricsProperties = new DbaasOpensearchMetricsProperties();

        metricsProperties.setEnabled(enabled().orElse(Boolean.TRUE));
        metricsProperties.setRequestsSeconds(
                requestsSeconds().toOpensearchClientRequestsSecondsMetricsProperties()
        );

        return metricsProperties;
    }
}
