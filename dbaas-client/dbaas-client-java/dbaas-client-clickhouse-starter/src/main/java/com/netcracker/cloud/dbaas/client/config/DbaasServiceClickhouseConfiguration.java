package org.qubership.cloud.dbaas.client.config;

import org.qubership.cloud.dbaas.client.entity.DbaasApiProperties;
import org.qubership.cloud.dbaas.client.management.DatabaseConfig;
import org.qubership.cloud.dbaas.client.management.DatabasePool;
import org.qubership.cloud.dbaas.client.management.DbaasClickhouseDatasource;
import org.qubership.cloud.dbaas.client.management.classifier.DbaaSClassifierBuilder;
import org.qubership.cloud.dbaas.client.management.classifier.DbaasClassifierFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.qubership.cloud.dbaas.client.config.DbaasClickhouseConfiguration.SERVICE_CLICKHOUSE_DATASOURCE;

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
