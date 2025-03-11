package org.qubership.cloud.core.quarkus.dbaas.datasource.metrics;

import org.qubership.cloud.dbaas.client.entity.database.PostgresDatabase;
import org.qubership.cloud.dbaas.client.entity.database.type.PostgresDBType;
import org.qubership.cloud.dbaas.client.exceptions.MetricsRegistrationException;
import org.qubership.cloud.dbaas.client.metrics.DatabaseMetricProperties;
import org.qubership.cloud.dbaas.client.metrics.MetricsProvider;
import io.agroal.api.AgroalDataSource;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PostgresMicrometerMetricsProvider implements MetricsProvider<PostgresDatabase> {
    public static final String DATASOURCE_PARAMETER = "datasource";
    public static final String SCHEMA_TAG = "schema";

    @Override
    public void registerMetrics(DatabaseMetricProperties databaseMetricProperties) throws MetricsRegistrationException {
        MeterRegistry meterRegistry = Metrics.globalRegistry;
        try {
            AgroalDataSource dataSource = (AgroalDataSource) databaseMetricProperties.getExtraParameters().get(DATASOURCE_PARAMETER);
            MeterBinder meterBinder = new AgroalDataSourceMetricsBinder(dataSource, databaseMetricProperties.getDatabaseName(),
                    databaseMetricProperties.getMetricTags().entrySet().stream().map(tag -> Tag.of(tag.getKey(), tag.getValue())).toList());
            meterBinder.bindTo(meterRegistry);
        } catch (ClassCastException e) {
            throw new MetricsRegistrationException("Unable to get postgres datasource instance for metric registration", e);
        }
    }

    @Override
    public Class getSupportedDatabaseType() {
        return PostgresDBType.INSTANCE.getDatabaseClass();
    }
}
