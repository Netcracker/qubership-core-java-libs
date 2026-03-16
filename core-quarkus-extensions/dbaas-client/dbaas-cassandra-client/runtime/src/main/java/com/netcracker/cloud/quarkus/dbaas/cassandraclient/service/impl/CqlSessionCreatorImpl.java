package com.netcracker.cloud.quarkus.dbaas.cassandraclient.service.impl;

import com.datastax.oss.driver.api.core.CqlSession;
import com.netcracker.cloud.dbaas.client.cassandra.entity.connection.CassandraDBConnection;
import com.netcracker.cloud.dbaas.client.cassandra.entity.database.CassandraDatabase;
import com.netcracker.cloud.dbaas.client.cassandra.migration.MigrationExecutor;
import com.netcracker.cloud.dbaas.client.cassandra.service.CassandraSessionBuilder;
import com.netcracker.cloud.quarkus.dbaas.cassandraclient.service.CqlSessionCreator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class CqlSessionCreatorImpl implements CqlSessionCreator {

    @Inject
    CassandraSessionBuilder cassandraSessionBuilder;
    @Inject
    MigrationExecutor migrationExecutor;

    @Override
    public CqlSession createSession(CassandraDBConnection connectionProperties) {
        CqlSession cqlSession = cassandraSessionBuilder.build(connectionProperties);
        migrationExecutor.migrate(cqlSession);
        return cqlSession;
    }

    @Override
    public CqlSession createSession(CassandraDatabase cassandraDatabase) {
        CqlSession cqlSession = cassandraSessionBuilder.build(cassandraDatabase);
        migrationExecutor.migrate(cqlSession);
        return cqlSession;
    }
}
