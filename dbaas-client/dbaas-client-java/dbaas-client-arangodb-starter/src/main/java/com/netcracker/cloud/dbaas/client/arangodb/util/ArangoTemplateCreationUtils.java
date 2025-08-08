package com.netcracker.cloud.dbaas.client.arangodb.util;

import com.arangodb.ArangoDB;
import com.arangodb.springframework.config.ArangoConfiguration;
import org.qubership.cloud.dbaas.client.arangodb.configuration.DbaasArangoDBConfigurationProperties;
import org.qubership.cloud.dbaas.client.arangodb.service.DbaasArangoTemplate;
import org.qubership.cloud.dbaas.client.management.ArangoDatabaseProvider;

import org.springframework.context.ApplicationContext;

public class ArangoTemplateCreationUtils implements ArangoConfiguration {

    private static final ArangoTemplateCreationUtils INSTANCE = new ArangoTemplateCreationUtils();

    @Override
    public ArangoDB.Builder arango() {
        return null;
    }

    @Override
    public String database() {
        return "";
    }

    public DbaasArangoTemplate createDbaasArangoTemplate(ArangoDatabaseProvider arangoDatabaseProvider, DbaasArangoDBConfigurationProperties dbaasArangoConfig, ApplicationContext applicationContext) throws Exception {
        return new DbaasArangoTemplate(arangoDatabaseProvider, arangoConverter(), resolverFactory(), dbaasArangoConfig, applicationContext);
    }

    public static ArangoTemplateCreationUtils getInstance() {
        return INSTANCE;
    }
}
