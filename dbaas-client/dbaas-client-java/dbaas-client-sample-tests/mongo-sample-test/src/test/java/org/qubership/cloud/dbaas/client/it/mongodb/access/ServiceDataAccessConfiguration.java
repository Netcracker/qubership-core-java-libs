package org.qubership.cloud.dbaas.client.it.mongodb.access;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import static org.qubership.cloud.dbaas.client.config.DbaasMongoConfiguration.SERVICE_MONGO_TEMPLATE;

@Configuration
@EnableMongoRepositories(
        basePackages = "org.qubership.cloud.dbaas.client.it.mongodb.service",
        mongoTemplateRef = SERVICE_MONGO_TEMPLATE)
public class ServiceDataAccessConfiguration {
}

