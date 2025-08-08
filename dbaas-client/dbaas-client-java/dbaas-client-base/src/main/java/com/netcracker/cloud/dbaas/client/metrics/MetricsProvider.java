package com.netcracker.cloud.dbaas.client.metrics;

import org.qubership.cloud.dbaas.client.entity.database.AbstractDatabase;
import org.qubership.cloud.dbaas.client.exceptions.MetricsRegistrationException;
import org.qubership.cloud.dbaas.client.management.SupportedDatabaseType;

public interface MetricsProvider<T extends AbstractDatabase> extends SupportedDatabaseType<T> {
    void registerMetrics(DatabaseMetricProperties metricProperties) throws MetricsRegistrationException;
}
