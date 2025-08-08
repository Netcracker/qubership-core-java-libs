package com.netcracker.cloud.dbaas.client.it.cassandra.config;


import org.qubership.cloud.dbaas.client.cassandra.entity.database.CassandraDatabase;
import org.qubership.cloud.dbaas.client.management.PostConnectProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DataInitializePostConnectProcessor implements PostConnectProcessor<CassandraDatabase> {
    @Override
    public void process(CassandraDatabase cassandraDatabase) {
        cassandraDatabase.getConnectionProperties().getSession().execute(
                "CREATE TABLE IF NOT EXISTS service_film (id uuid primary key, title text, year int)"
        );
        log.debug("service_film table has been created");

        cassandraDatabase.getConnectionProperties().getSession().execute(
                "CREATE TABLE IF NOT EXISTS tenant_film (id uuid primary key, title text, year int)"
        );
        log.debug("tenant_film table has been created");
    }

    @Override
    public Class<CassandraDatabase> getSupportedDatabaseType() {
        return CassandraDatabase.class;
    }
}

