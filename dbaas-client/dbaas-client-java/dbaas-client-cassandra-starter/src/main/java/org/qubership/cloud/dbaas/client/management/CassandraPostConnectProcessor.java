package org.qubership.cloud.dbaas.client.management;

import com.datastax.oss.driver.api.core.CqlSession;
import org.qubership.cloud.dbaas.client.cassandra.entity.database.CassandraDatabase;
import org.qubership.cloud.dbaas.client.cassandra.migration.MigrationExecutor;
import org.qubership.cloud.dbaas.client.cassandra.service.CassandraSessionBuilder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CassandraPostConnectProcessor implements PostConnectProcessor<CassandraDatabase> {

    private CassandraSessionBuilder cassandraSessionBuilder;
    private MigrationExecutor migrationExecutor;

    public CassandraPostConnectProcessor(CassandraSessionBuilder cassandraSessionBuilder,
                                         MigrationExecutor migrationExecutor) {
        this.cassandraSessionBuilder = cassandraSessionBuilder;
        this.migrationExecutor = migrationExecutor;
    }

    @Override
    public void process(CassandraDatabase database) {
        CqlSession session = cassandraSessionBuilder.build(database);
        migrationExecutor.migrate(session);
        database.getConnectionProperties().setSession(session);
    }

    @Override
    public Class<CassandraDatabase> getSupportedDatabaseType() {
        return CassandraDatabase.class;
    }
}
