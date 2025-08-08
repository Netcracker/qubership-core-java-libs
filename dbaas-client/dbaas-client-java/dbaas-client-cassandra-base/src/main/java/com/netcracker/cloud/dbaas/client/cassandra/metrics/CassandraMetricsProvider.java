package com.netcracker.cloud.dbaas.client.cassandra.metrics;

import com.datastax.oss.driver.api.core.config.ProgrammaticDriverConfigLoaderBuilder;
import org.qubership.cloud.dbaas.client.cassandra.entity.database.CassandraDatabase;
import org.qubership.cloud.dbaas.client.cassandra.entity.database.type.CassandraDBType;
import org.qubership.cloud.dbaas.client.exceptions.MetricsRegistrationException;
import org.qubership.cloud.dbaas.client.metrics.DatabaseMetricProperties;
import org.qubership.cloud.dbaas.client.metrics.MetricsProvider;

import java.util.Map;

public class CassandraMetricsProvider implements MetricsProvider<CassandraDatabase> {

    @Override
    public Class getSupportedDatabaseType() {
        return CassandraDBType.INSTANCE.getDatabaseClass();
    }

    @Override
    public void registerMetrics(DatabaseMetricProperties metricProperties) throws MetricsRegistrationException {
        ProgrammaticDriverConfigLoaderBuilder configLoader = (ProgrammaticDriverConfigLoaderBuilder) metricProperties.getExtraParameters().get("config_loader");
        Map<String, String> metricTags = metricProperties.getMetricTags();
        metricTags.put("name", metricProperties.getDatabaseName());
        configLoader.withStringMap(DbaasTaggingMetricIdGenerator.DBAAS_METRICS_TAGS, metricTags);
    }
}
