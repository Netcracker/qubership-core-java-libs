package com.netcracker.cloud.dbaas.client.config;

import com.netcracker.cloud.dbaas.client.entity.database.AbstractDatabase;
import com.netcracker.cloud.dbaas.client.metrics.DbaaSMetricsRegistrar;
import com.netcracker.cloud.dbaas.client.metrics.MetricsProvider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class MetricsConfiguration {

    @Bean
    public DbaaSMetricsRegistrar metricsRegistrar(@Autowired List<MetricsProvider<? extends AbstractDatabase>> metricsProviders) {
        return new DbaaSMetricsRegistrar(metricsProviders);
    }
}
