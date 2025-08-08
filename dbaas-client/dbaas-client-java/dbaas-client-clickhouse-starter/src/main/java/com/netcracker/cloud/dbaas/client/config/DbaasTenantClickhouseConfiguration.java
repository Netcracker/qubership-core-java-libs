package com.netcracker.cloud.dbaas.client.config;

import com.netcracker.cloud.dbaas.client.entity.DbaasApiProperties;
import com.netcracker.cloud.dbaas.client.management.DatabaseConfig;
import com.netcracker.cloud.dbaas.client.management.DatabasePool;
import com.netcracker.cloud.dbaas.client.management.DbaasClickhouseDatasource;
import com.netcracker.cloud.dbaas.client.management.classifier.DbaaSClassifierBuilder;
import com.netcracker.cloud.dbaas.client.management.classifier.DbaasClassifierFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static com.netcracker.cloud.dbaas.client.config.DbaasClickhouseConfiguration.*;

@Configuration
@Import({DbaasClickhouseConfiguration.class})
public class DbaasTenantClickhouseConfiguration {

    @Bean(name = {TENANT_CLICKHOUSE_DATASOURCE})
    @ConditionalOnMissingBean(name = TENANT_CLICKHOUSE_DATASOURCE)
    public DbaasClickhouseDatasource tenantClickhouseDbFactory(DatabasePool databasePool,
                                                     DbaasClassifierFactory dbaasClassifierFactory,
                                                     DbaasApiProperties clickhouseDbaasApiProperties) {
        DbaaSClassifierBuilder classifierBuilder = dbaasClassifierFactory.newTenantClassifierBuilder();
        DatabaseConfig databaseConfig = DatabaseConfig.builder()
                .dbNamePrefix(clickhouseDbaasApiProperties.getDbPrefix())
                .userRole(clickhouseDbaasApiProperties.getRuntimeUserRole())
                .build();
        return new DbaasClickhouseDatasource(classifierBuilder, databasePool, databaseConfig);
    }
}
