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

import static com.netcracker.cloud.dbaas.client.config.DbaasClickhouseConfiguration.SERVICE_CLICKHOUSE_DATASOURCE;

@Configuration
@Import({DbaasClickhouseConfiguration.class})
public class DbaasServiceClickhouseConfiguration {

    @Bean(name = {SERVICE_CLICKHOUSE_DATASOURCE})
    @ConditionalOnMissingBean(name = SERVICE_CLICKHOUSE_DATASOURCE)
    public DbaasClickhouseDatasource serviceClickhouseDbFactory(DatabasePool databasePool,
                                                      DbaasClassifierFactory dbaasClassifierFactory,
                                                      DbaasApiProperties clickhouseDbaasApiProperties) {
        DbaaSClassifierBuilder classifierBuilder = dbaasClassifierFactory.newServiceClassifierBuilder();
        DatabaseConfig databaseConfig = DatabaseConfig.builder()
                .dbNamePrefix(clickhouseDbaasApiProperties.getDbPrefix())
                .userRole(clickhouseDbaasApiProperties.getRuntimeUserRole())
                .build();
        return new DbaasClickhouseDatasource(classifierBuilder, databasePool, databaseConfig);
    }

}
