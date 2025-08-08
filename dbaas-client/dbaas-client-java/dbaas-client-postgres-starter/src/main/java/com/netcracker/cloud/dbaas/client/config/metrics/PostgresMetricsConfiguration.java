package com.netcracker.cloud.dbaas.client.config.metrics;

import org.qubership.cloud.dbaas.client.config.DbaasPostgresConfiguration;
import org.qubership.cloud.dbaas.client.config.MetricsConfiguration;
import org.qubership.cloud.dbaas.client.entity.database.PostgresDatabase;
import org.qubership.cloud.dbaas.client.metrics.MetricsProvider;
import org.qubership.cloud.dbaas.client.metrics.PostgresMetricsProvider;
import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.metadata.DataSourcePoolMetadataProvidersConfiguration;
import org.springframework.boot.jdbc.metadata.DataSourcePoolMetadataProvider;
import org.springframework.boot.jdbc.metadata.HikariDataSourcePoolMetadata;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

@AutoConfiguration(
        after = {SimpleMetricsExportAutoConfiguration.class,
                CompositeMeterRegistryAutoConfiguration.class,
                DataSourcePoolMetadataProvidersConfiguration.class})
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
@Import(MetricsConfiguration.class)
@ConditionalOnProperty(value = "dbaas.postgres.metrics.enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnBean({MeterRegistry.class, DbaasPostgresConfiguration.class})
public class PostgresMetricsConfiguration {

    @Bean(name = "hikariPoolDataSourceMetadataProvider")
    @ConditionalOnMissingBean(name = "hikariPoolDataSourceMetadataProvider")
    public DataSourcePoolMetadataProvider dataSourceMetadataProvider() {
        return dataSource -> {
            if (dataSource instanceof HikariDataSource hikariDataSource) {
                return new HikariDataSourcePoolMetadata(hikariDataSource);
            }
            return null;
        };
    }

    @Bean
    public MetricsProvider<PostgresDatabase> postgresMetricsProvider(MeterRegistry registry,
                                                                     @Qualifier("hikariPoolDataSourceMetadataProvider") DataSourcePoolMetadataProvider dataSourcePoolMetadataProvider) {
        return new PostgresMetricsProvider(registry, dataSourcePoolMetadataProvider);
    }

}
