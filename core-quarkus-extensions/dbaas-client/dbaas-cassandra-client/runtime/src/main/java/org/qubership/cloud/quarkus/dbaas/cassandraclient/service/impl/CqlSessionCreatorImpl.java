package org.qubership.cloud.quarkus.dbaas.cassandraclient.service.impl;

import com.datastax.oss.driver.api.core.CqlSession;
import org.qubership.cloud.dbaas.client.cassandra.entity.connection.CassandraDBConnection;
import org.qubership.cloud.dbaas.client.cassandra.entity.database.CassandraDatabase;
import org.qubership.cloud.dbaas.client.cassandra.migration.MigrationExecutor;
import org.qubership.cloud.dbaas.client.cassandra.service.CassandraSessionBuilder;
import org.qubership.cloud.quarkus.dbaas.cassandraclient.service.CqlSessionCreator;
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
