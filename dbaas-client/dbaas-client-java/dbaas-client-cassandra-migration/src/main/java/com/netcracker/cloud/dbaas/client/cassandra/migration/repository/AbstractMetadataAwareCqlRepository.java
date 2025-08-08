package com.netcracker.cloud.dbaas.client.cassandra.migration.repository;

import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.metadata.schema.KeyspaceMetadata;
import org.qubership.cloud.dbaas.client.cassandra.migration.exception.SchemaMigrationException;
import org.qubership.cloud.dbaas.client.cassandra.migration.session.SchemaMigrationSession;

public abstract class AbstractMetadataAwareCqlRepository extends AbstractCqlRepository {
    protected final KeyspaceMetadata keyspaceMetadata;

    protected AbstractMetadataAwareCqlRepository(SchemaMigrationSession session) {
        super(session);
        CqlIdentifier keyspace = session.getKeyspace()
                .orElseThrow(() -> new SchemaMigrationException("Provided CqlSession instance does not have keyspace set."));
        this.keyspaceMetadata = session.getMetadata().getKeyspace(keyspace)
                .orElseThrow(() -> new SchemaMigrationException(String.format(
                        "Unable to get metadata for not existing keyspace %s.", keyspace
                )));
    }

    protected boolean tableExists(String tableName) {
        return this.keyspaceMetadata.getTable(tableName).isPresent();
    }
}
