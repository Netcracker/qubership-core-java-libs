package com.netcracker.cloud.dbaas.client.config;


import com.netcracker.cloud.dbaas.client.entity.DbaasApiProperties;
import com.netcracker.cloud.dbaas.client.management.DatabasePool;
import com.netcracker.cloud.dbaas.client.management.DbaasPostgresqlDatasourceBuilder;
import com.netcracker.cloud.dbaas.client.management.PostgresDatasourceCreator;
import com.netcracker.cloud.dbaas.client.metrics.DbaaSMetricsRegistrar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({DbaasPostgresDataSourceProperties.class})
@EnableDbaasDefault
public class DbaasPostgresConfiguration {

    public static final String TENANT_POSTGRES_DATASOURCE = "tenantPostgresDatasource";
    public static final String SERVICE_POSTGRES_DATASOURCE = "servicePostgresDatasource";
    public static final String DATASOURCE = "dataSource";

    @Bean
    @ConditionalOnMissingBean
    public PostgresDatasourceCreator postgresDatasourceCreator(
            DbaasPostgresDataSourceProperties dbaasDsProperties,
            @Autowired(required = false) DbaaSMetricsRegistrar metricsRegistrar) {
        return new PostgresDatasourceCreator(dbaasDsProperties, metricsRegistrar);
    }

    @Bean("postgresDbaasApiProperties")
    @ConfigurationProperties("dbaas.api.postgres")
    public DbaasApiProperties dbaasApiProperties() {
        return new DbaasApiProperties();
    }

    @Bean
    public DbaasPostgresqlDatasourceBuilder dbaasPostgresqlDatasourceBuilder(DatabasePool databasePool) {
        return new DbaasPostgresqlDatasourceBuilder(databasePool);
    }
}
