package com.netcracker.cloud.dbaas.client.config;

import com.mongodb.MongoClientSettings;
import org.qubership.cloud.dbaas.client.entity.DbaasApiProperties;
import org.qubership.cloud.dbaas.client.management.MongoPostConnectProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@EnableDbaasDefault
@EnableConfigurationProperties
@Configuration
public class DbaasMongoConfiguration {
    public static final String TENANT_MONGO_DB_FACTORY = "tenantMongoDbFactory";
    public static final String SERVICE_MONGO_DB_FACTORY = "serviceMongoDbFactory";
    public static final String SERVICE_MONGO_TEMPLATE = "serviceMongoTemplate";
    public static final String TENANT_MONGO_TEMPLATE = "tenantMongoTemplate";
    public static final String SERVICE_MONGO_CONVERTER = "serviceMongoConverter";
    public static final String TENANT_MONGO_CONVERTER = "tenantMongoConverter";

    // the list of mandatory post connect processors
    @Bean
    public MongoPostConnectProcessor mongoPostConnectProcessor(MongoClientSettings mongoClientSettings) {
        return new MongoPostConnectProcessor(mongoClientSettings);
    }

    @Bean
    @ConditionalOnMissingBean
    MongoClientSettings mongoClientSettings() {
        return MongoClientSettings.builder().build();
    }

    @Bean("mongoDbaasApiProperties")
    @ConfigurationProperties("dbaas.api.mongo")
    public DbaasApiProperties dbaasApiProperties() {
        return new DbaasApiProperties();
    }
}
