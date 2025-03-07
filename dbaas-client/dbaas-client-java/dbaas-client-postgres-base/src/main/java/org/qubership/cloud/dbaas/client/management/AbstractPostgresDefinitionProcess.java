package org.qubership.cloud.dbaas.client.management;

import org.qubership.cloud.dbaas.client.entity.database.PostgresDatabase;

public abstract class AbstractPostgresDefinitionProcess implements DatabaseDefinitionProcessor<PostgresDatabase> {
    @Override
    public Class<PostgresDatabase> getSupportedDatabaseType() {
        return PostgresDatabase.class;
    }
}
