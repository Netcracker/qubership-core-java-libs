package com.netcracker.cloud.dbaas.client.it.mongodb.access;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import static com.netcracker.cloud.dbaas.client.config.DbaasMongoConfiguration.TENANT_MONGO_TEMPLATE;

@Configuration
@EnableMongoRepositories(
        basePackages = "com.netcracker.cloud.dbaas.client.it.mongodb.tenant",
        mongoTemplateRef = TENANT_MONGO_TEMPLATE)
public class TenantDataAccessConfiguration {
}

