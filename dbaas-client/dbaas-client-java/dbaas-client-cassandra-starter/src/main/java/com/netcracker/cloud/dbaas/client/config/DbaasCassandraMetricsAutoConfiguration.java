package com.netcracker.cloud.dbaas.client.config;

import com.netcracker.cloud.dbaas.client.cassandra.entity.database.CassandraDatabase;
import com.netcracker.cloud.dbaas.client.cassandra.metrics.CassandraMetricsProvider;
import com.netcracker.cloud.dbaas.client.metrics.MetricsProvider;
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
@ConditionalOnProperty(value = "dbaas.cassandra.metrics.enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnBean({MeterRegistry.class, DbaasCassandraConfiguration.class})
public class DbaasCassandraMetricsAutoConfiguration {

    @Bean
    public MetricsProvider<CassandraDatabase> cassandraMetricsProvider() {
        return new CassandraMetricsProvider();
    }
}
