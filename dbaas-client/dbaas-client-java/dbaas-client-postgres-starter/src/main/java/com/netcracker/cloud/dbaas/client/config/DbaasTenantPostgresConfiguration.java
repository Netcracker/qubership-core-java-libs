package com.netcracker.cloud.dbaas.client.config;

import org.qubership.cloud.dbaas.client.entity.DbaasApiProperties;
import org.qubership.cloud.dbaas.client.entity.settings.PostgresSettings;
import org.qubership.cloud.dbaas.client.management.DatabaseConfig;
import org.qubership.cloud.dbaas.client.management.DatabasePool;
import org.qubership.cloud.dbaas.client.management.DbaasPostgresProxyDataSource;
import org.qubership.cloud.dbaas.client.management.classifier.DbaasClassifierFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.util.CollectionUtils;

import javax.sql.DataSource;

import static org.qubership.cloud.dbaas.client.config.DbaasPostgresConfiguration.TENANT_POSTGRES_DATASOURCE;

@Configuration
@Import({DbaasPostgresConfiguration.class})
public class DbaasTenantPostgresConfiguration {

    @Bean(name = {TENANT_POSTGRES_DATASOURCE})
    @ConditionalOnMissingBean(name = TENANT_POSTGRES_DATASOURCE)
    public static DataSource tenantPostgresDatasource(DatabasePool databasePool,
                                                      DbaasClassifierFactory classifierFactory,
                                                      DbaasApiProperties postgresDbaasApiProperties) {
        PostgresSettings databaseSettings = null;
        if (!CollectionUtils.isEmpty(postgresDbaasApiProperties.getDatabaseSettings(DbaasApiProperties.DbScope.TENANT))) {
            databaseSettings = new PostgresSettings(postgresDbaasApiProperties.getDatabaseSettings(DbaasApiProperties.DbScope.TENANT));
        }
        DatabaseConfig databaseConfig = DatabaseConfig.builder()
                .userRole(postgresDbaasApiProperties.getRuntimeUserRole())
                .dbNamePrefix(postgresDbaasApiProperties.getDbPrefix())
                .databaseSettings(databaseSettings).build();

        return new DbaasPostgresProxyDataSource(databasePool, classifierFactory.newTenantClassifierBuilder(), databaseConfig);
    }
}
