package com.netcracker.cloud.dbaas.client.metrics;

import com.netcracker.cloud.dbaas.client.entity.database.PostgresDatabase;
import com.netcracker.cloud.dbaas.client.entity.database.type.PostgresDBType;
import com.netcracker.cloud.dbaas.client.exceptions.MetricsRegistrationException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;

import org.springframework.boot.actuate.metrics.jdbc.DataSourcePoolMetrics;
import org.springframework.boot.jdbc.metadata.DataSourcePoolMetadataProvider;

import javax.sql.DataSource;

public class PostgresMetricsProvider implements MetricsProvider<PostgresDatabase> {
    public static final String DATASOURCE_PARAMETER = "datasource";
    public static final String SCHEMA_TAG = "schema";

    private MeterRegistry registry;
    private DataSourcePoolMetadataProvider metadataProvider;

    public PostgresMetricsProvider(MeterRegistry registry,
                                   DataSourcePoolMetadataProvider dataSourcePoolMetadataProvider) {
        this.registry = registry;
        this.metadataProvider = dataSourcePoolMetadataProvider;
    }

    @Override
    public Class getSupportedDatabaseType() {
        return PostgresDBType.INSTANCE.getDatabaseClass();
    }


    @Override
    public void registerMetrics(DatabaseMetricProperties metricProperties) throws MetricsRegistrationException {
        try {
            DataSource dataSource = (DataSource) metricProperties.getExtraParameters().get(DATASOURCE_PARAMETER);
            MeterBinder meterBinder = new DataSourcePoolMetrics(dataSource, metadataProvider, metricProperties.getDatabaseName(),
                    metricProperties.getMetricTags().entrySet().stream().map(tag -> Tag.of(tag.getKey(), tag.getValue())).toList());
            meterBinder.bindTo(registry);
        } catch (ClassCastException e) {
            throw new MetricsRegistrationException("Unable to get postgres datasource instance for metric registration", e);
        }
    }
}
