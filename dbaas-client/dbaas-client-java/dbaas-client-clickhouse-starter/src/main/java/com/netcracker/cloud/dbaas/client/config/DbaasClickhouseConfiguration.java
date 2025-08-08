package com.netcracker.cloud.dbaas.client.config;

import org.qubership.cloud.dbaas.client.entity.DbaasApiProperties;
import org.qubership.cloud.dbaas.client.entity.database.ClickhouseDatasourceCreator;
import org.qubership.cloud.dbaas.client.entity.database.DbaasClickhouseDatasourceProperties;
import org.qubership.cloud.dbaas.client.management.DatabasePool;
import org.qubership.cloud.dbaas.client.management.DbaasClickhouseDatasourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@EnableDbaasDefault
@Configuration
@EnableConfigurationProperties
public class DbaasClickhouseConfiguration {
    public static final String TENANT_CLICKHOUSE_DATASOURCE = "tenantClickhouseDatasource";
    public static final String SERVICE_CLICKHOUSE_DATASOURCE = "serviceClickhouseDatasource";

    @Bean
    @ConditionalOnMissingBean
    public ClickhouseDatasourceCreator clickhouseDatasourceCreator(
            DbaasClickhouseDatasourceProperties dbaasClickhouseDatasourceProperties) {
        return new ClickhouseDatasourceCreator(dbaasClickhouseDatasourceProperties);
    }

    @Bean
    public DbaasClickhouseDatasourceBuilder dbaasClickhouseDatasourceBuilder(DatabasePool databasePool) {
        return new DbaasClickhouseDatasourceBuilder(databasePool);
    }

    @Bean("dbaasClickhouseDatasourceProperties")
    @ConfigurationProperties("dbaas.clickhouse")
    public DbaasClickhouseDatasourceProperties dbaasClickhouseDatasourceProperties() {
        return new DbaasClickhouseDatasourceProperties();
    }

    @Bean("clickhouseDbaasApiProperties")
    @ConfigurationProperties("dbaas.api.clickhouse")
    public DbaasApiProperties dbaasApiProperties() {
        return new DbaasApiProperties();
    }
}
