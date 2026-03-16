package com.netcracker.cloud.dbaas.client.management;

import com.netcracker.cloud.dbaas.client.entity.database.PostgresDatabase;

public abstract class AbstractPostgresDefinitionProcess implements DatabaseDefinitionProcessor<PostgresDatabase> {
    @Override
    public Class<PostgresDatabase> getSupportedDatabaseType() {
        return PostgresDatabase.class;
    }
}
