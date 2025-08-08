package com.netcracker.cloud.dbaas.client.arangodb.configuration;

import com.arangodb.springframework.core.template.ArangoTemplate;
import org.qubership.cloud.dbaas.client.arangodb.util.ArangoTemplateCreationUtils;
import org.qubership.cloud.dbaas.client.config.EnableDbaasDefault;
import org.qubership.cloud.dbaas.client.management.ArangoDatabaseProvider;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

@Configuration
@EnableDbaasDefault
@EnableConfigurationProperties(DbaasArangoDBConfigurationProperties.class)
@Import({ServiceDbaasArangoConfiguration.class, CommonArangoTemplateConfiguration.class})
public class ServiceArangoTemplateConfiguration {

    public static final String SERVICE_ARANGO_TEMPLATE = "dbaasServiceArangoTemplate";

    protected final DbaasArangoDBConfigurationProperties dbaasArangoConfig;
    protected final ArangoDatabaseProvider serviceArangoDatabaseProvider;
    protected final ApplicationContext applicationContext;

    public ServiceArangoTemplateConfiguration(DbaasArangoDBConfigurationProperties dbaasArangoConfig,
                                              @Qualifier(ServiceDbaasArangoConfiguration.SERVICE_ARANGODB_PROVIDER) ArangoDatabaseProvider serviceArangoDatabaseProvider,
                                              ApplicationContext applicationContext) {
        this.dbaasArangoConfig = dbaasArangoConfig;
        this.serviceArangoDatabaseProvider = serviceArangoDatabaseProvider;
        this.applicationContext = applicationContext;
    }

    @Primary
    @Bean(name = SERVICE_ARANGO_TEMPLATE)
    public ArangoTemplate arangoTemplate() throws Exception {
        return ArangoTemplateCreationUtils.getInstance().createDbaasArangoTemplate(serviceArangoDatabaseProvider, dbaasArangoConfig, applicationContext);
    }
}
