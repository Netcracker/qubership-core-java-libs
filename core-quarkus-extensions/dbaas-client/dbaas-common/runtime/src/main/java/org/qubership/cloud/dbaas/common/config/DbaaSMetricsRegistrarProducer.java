package org.qubership.cloud.dbaas.common.config;

import org.qubership.cloud.dbaas.client.entity.database.AbstractDatabase;
import org.qubership.cloud.dbaas.client.metrics.DbaaSMetricsRegistrar;
import org.qubership.cloud.dbaas.client.metrics.MetricsProvider;
import io.quarkus.arc.All;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Singleton
public class DbaaSMetricsRegistrarProducer {

    @Produces
    @DefaultBean
    public DbaaSMetricsRegistrar dbaaSMetricsRegistrar(@All List<MetricsProvider<? extends AbstractDatabase>> metricsProviders) {
        return new DbaaSMetricsRegistrar(metricsProviders);
    }
}
