package com.netcracker.cloud.dbaas.client.metrics;

import com.netcracker.cloud.dbaas.client.entity.database.AbstractDatabase;
import com.netcracker.cloud.dbaas.client.exceptions.MetricsRegistrationException;
import com.netcracker.cloud.dbaas.client.management.SupportedDatabaseType;

public interface MetricsProvider<T extends AbstractDatabase> extends SupportedDatabaseType<T> {
    void registerMetrics(DatabaseMetricProperties metricProperties) throws MetricsRegistrationException;
}
