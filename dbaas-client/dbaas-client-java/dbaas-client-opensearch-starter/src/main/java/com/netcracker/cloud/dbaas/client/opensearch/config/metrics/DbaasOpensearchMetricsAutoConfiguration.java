package com.netcracker.cloud.dbaas.client.opensearch.config.metrics;

import com.netcracker.cloud.dbaas.client.config.MetricsConfiguration;
import com.netcracker.cloud.dbaas.client.metrics.MetricsProvider;
import com.netcracker.cloud.dbaas.client.opensearch.config.DbaaSOpensearchConfigurationProperty;
import com.netcracker.cloud.dbaas.client.opensearch.config.DbaasOpensearchConfiguration;
import com.netcracker.cloud.dbaas.client.opensearch.entity.OpensearchIndex;
import com.netcracker.cloud.dbaas.client.opensearch.metrics.OpensearchMetricsProvider;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.simple.SimpleMetricsExportAutoConfiguration;
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
