package org.qubership.cloud.quarkus.dbaas.opensearch.client.config.properties.metrics;

import org.qubership.cloud.dbaas.client.opensearch.entity.DbaasOpensearchMetricsProperties;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Optional;

@Getter
@Accessors(fluent = true)
@ConfigGroup
public class OpensearchMetricsProperties {

    /**
     * Enabling configuring metrics for DBaaS Opensearch client. Default value: true.
     */
    @ConfigItem(name = "enabled")
    Optional<Boolean> enabled;

    /**
     * Properties for configuring {@link org.qubership.cloud.dbaas.client.opensearch.metrics.OpensearchMetricsProvider#REQUESTS_SECONDS_METRIC_NAME} metric.
     */
    @ConfigItem(name = "requests-seconds")
    RequestsSecondsMetricProperties requestsSeconds;

    public DbaasOpensearchMetricsProperties toDbaasOpensearchMetricsProperties() {
        var metricsProperties = new DbaasOpensearchMetricsProperties();

        metricsProperties.setEnabled(enabled.orElse(Boolean.TRUE));
        metricsProperties.setRequestsSeconds(requestsSeconds.toOpensearchClientRequestsSecondsMetricsProperties());

        return metricsProperties;
    }
}
