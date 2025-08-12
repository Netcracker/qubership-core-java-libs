package com.netcracker.cloud.dbaas.client.arangodb.configuration;

import com.netcracker.cloud.dbaas.client.entity.DbaasApiProperties;
import com.netcracker.cloud.dbaas.client.management.ArangoDatabaseProvider;
import com.netcracker.cloud.dbaas.client.management.DatabaseConfig;
import com.netcracker.cloud.dbaas.client.management.DatabasePool;
import com.netcracker.cloud.dbaas.client.management.classifier.DbaasClassifierFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TenantDbaasArangoConfiguration {

    public static final String TENANT_ARANGODB_PROVIDER = "tenantArangoDbProvider";

    @Bean(TENANT_ARANGODB_PROVIDER)
    @ConditionalOnMissingBean(name = TENANT_ARANGODB_PROVIDER)
    public ArangoDatabaseProvider tenantArangoDatabaseProvider(DatabasePool databasePool,
                                                               DbaasClassifierFactory classifierFactory,
                                                               DbaasApiProperties arangodbDbaasApiProperties) {
        DatabaseConfig databaseConfig = DatabaseConfig.builder()
                .userRole(arangodbDbaasApiProperties.getRuntimeUserRole())
                .dbNamePrefix(arangodbDbaasApiProperties.getDbPrefix())
                .build();
        return new ArangoDatabaseProvider(databasePool, classifierFactory.newTenantClassifierBuilder(), databaseConfig, arangodbDbaasApiProperties.getRetryAttempts(), arangodbDbaasApiProperties.getRetryDelay());
    }
}
