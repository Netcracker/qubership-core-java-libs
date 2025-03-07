package org.qubership.cloud.dbaas.client.cassandra.migration.repository;

import org.qubership.cloud.dbaas.client.cassandra.migration.model.settings.SchemaMigrationSettings;
import org.qubership.cloud.dbaas.client.cassandra.migration.session.SchemaMigrationSession;

public class SchemaMigrationPreparationRepository extends AbstractCqlRepository {
    private static final String CREATE_LOCK_TABLE_QUERY = """
            create table if not exists %s (
                name text,
                lockUntil timestamp,
                lockedAt timestamp,
                lockedBy text,
                primary key (name)
            )
            """;

    private static final String CREATE_SCHEMA_HISTORY_TABLE_QUERY = """
            create table if not exists %s (
                installed_rank int,
                version        text,
                description    text,
                type           text,
                script         text,
                checksum       bigint,
                installed_by   text,
                installed_on   timestamp,
                execution_time bigint,
                success        boolean,
                extra_info     text,
                            
                primary key (installed_rank)
            )
            """;

    private final SchemaMigrationSettings schemaMigrationSettings;

    public SchemaMigrationPreparationRepository(
            SchemaMigrationSession session,
            SchemaMigrationSettings schemaMigrationSettings
    ) {
        super(session);
        this.schemaMigrationSettings = schemaMigrationSettings;
    }

    public void createLockTable() {
        session.execute(
                String.format(CREATE_LOCK_TABLE_QUERY, schemaMigrationSettings.lock().tableName())
        );
    }

    public void createSchemaHistoryTable() {
        session.execute(
                String.format(CREATE_SCHEMA_HISTORY_TABLE_QUERY, schemaMigrationSettings.schemaHistoryTableName())
        );
    }
}
