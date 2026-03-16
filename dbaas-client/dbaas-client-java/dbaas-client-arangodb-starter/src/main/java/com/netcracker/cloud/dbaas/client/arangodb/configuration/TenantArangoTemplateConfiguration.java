package com.netcracker.cloud.dbaas.client.arangodb.configuration;

import com.arangodb.springframework.core.template.ArangoTemplate;
import com.netcracker.cloud.dbaas.client.arangodb.util.ArangoTemplateCreationUtils;
import com.netcracker.cloud.dbaas.client.management.ArangoDatabaseProvider;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@EnableConfigurationProperties(DbaasArangoDBConfigurationProperties.class)
@Import({TenantDbaasArangoConfiguration.class, CommonArangoTemplateConfiguration.class, ServiceArangoTemplateConfiguration.class})
public class TenantArangoTemplateConfiguration {

    public static final String TENANT_ARANGO_TEMPLATE = "dbaasTenantArangoTemplate";

    protected final DbaasArangoDBConfigurationProperties dbaasArangoConfig;
    protected final ArangoDatabaseProvider tenantArangoDatabaseProvider;
    protected final ApplicationContext applicationContext;

    public TenantArangoTemplateConfiguration(DbaasArangoDBConfigurationProperties dbaasArangoConfig,
                                             @Qualifier(TenantDbaasArangoConfiguration.TENANT_ARANGODB_PROVIDER) ArangoDatabaseProvider tenantArangoDatabaseProvider,
                                             ApplicationContext applicationContext) {
        this.dbaasArangoConfig = dbaasArangoConfig;
        this.tenantArangoDatabaseProvider = tenantArangoDatabaseProvider;
        this.applicationContext = applicationContext;
    }

    @Bean(TENANT_ARANGO_TEMPLATE)
    @ConditionalOnMissingBean(name = TENANT_ARANGO_TEMPLATE)
    public ArangoTemplate tenantArangoTemplate() throws Exception {
        return ArangoTemplateCreationUtils.getInstance().createDbaasArangoTemplate(tenantArangoDatabaseProvider, dbaasArangoConfig, applicationContext);
    }
}
