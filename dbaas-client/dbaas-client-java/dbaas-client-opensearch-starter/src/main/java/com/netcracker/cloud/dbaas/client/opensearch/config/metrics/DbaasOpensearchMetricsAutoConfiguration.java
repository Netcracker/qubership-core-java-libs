package org.qubership.cloud.dbaas.client.opensearch.config.metrics;

import org.qubership.cloud.dbaas.client.config.MetricsConfiguration;
import org.qubership.cloud.dbaas.client.metrics.MetricsProvider;
import org.qubership.cloud.dbaas.client.opensearch.config.DbaaSOpensearchConfigurationProperty;
import org.qubership.cloud.dbaas.client.opensearch.config.DbaasOpensearchConfiguration;
import org.qubership.cloud.dbaas.client.opensearch.entity.OpensearchIndex;
import org.qubership.cloud.dbaas.client.opensearch.metrics.OpensearchMetricsProvider;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

@AutoConfiguration(
    after = {SimpleMetricsExportAutoConfiguration.class,
            CompositeMeterRegistryAutoConfiguration.class})
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
@Import(MetricsConfiguration.class)
@ConditionalOnProperty(value = "dbaas.opensearch.metrics.enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnBean({MeterRegistry.class, DbaasOpensearchConfiguration.class})
public class DbaasOpensearchMetricsAutoConfiguration {

    @Bean
    public MetricsProvider<OpensearchIndex> opensearchMetricsProvider(MeterRegistry metricRegistry,
                                                                      DbaaSOpensearchConfigurationProperty opensearchConfigurationProperties) {
        return new OpensearchMetricsProvider(metricRegistry, opensearchConfigurationProperties.getMetrics());
    }
}
