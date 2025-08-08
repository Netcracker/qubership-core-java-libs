package com.netcracker.cloud.dbaas.client.it.cassandra.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;

import static org.qubership.cloud.dbaas.client.config.DbaasCassandraConfiguration.*;

@Configuration
@EnableCassandraRepositories(
        basePackages = "org.qubership.cloud.dbaas.client.it.cassandra.tenant",
        cassandraTemplateRef = TENANT_CASSANDRA_TEMPLATE)
public class TenantDataAccessConfiguration {
}

