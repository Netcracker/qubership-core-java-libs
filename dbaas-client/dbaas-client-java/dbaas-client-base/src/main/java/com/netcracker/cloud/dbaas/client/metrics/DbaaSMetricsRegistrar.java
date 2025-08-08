package org.qubership.cloud.dbaas.client.metrics;

import org.qubership.cloud.dbaas.client.entity.database.AbstractDatabase;
import org.qubership.cloud.dbaas.client.entity.database.type.DatabaseType;
import org.qubership.cloud.dbaas.client.exceptions.MetricsRegistrationException;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class DbaaSMetricsRegistrar {

    private Map<Class<? extends AbstractDatabase>, MetricsProvider<? extends AbstractDatabase>> metricsProviders;

    public DbaaSMetricsRegistrar(List<MetricsProvider<? extends AbstractDatabase>> metricsProviders) {
        this.metricsProviders = metricsProviders != null ?
                metricsProviders.stream().collect(Collectors.toMap(MetricsProvider::getSupportedDatabaseType, Function.identity())) :
                new HashMap<>();
    }

    public <T, D extends AbstractDatabase<T>> void registerMetrics(DatabaseType<T, D> databaseType, DatabaseMetricProperties metricProperties) {
        MetricsProvider<? extends AbstractDatabase> metricsProvider = metricsProviders.get(databaseType.getDatabaseClass());
        if (metricsProvider == null) {
            log.warn("No metrics provider has been registered for database type {}", databaseType);
        } else {
            try {
                metricsProvider.registerMetrics(metricProperties);
            } catch (MetricsRegistrationException e) {
                log.error("Failed to register metrics for database", e);
            }
        }
    }
}
