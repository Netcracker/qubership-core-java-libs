package com.netcracker.cloud.dbaas.client.entity.postgres;

import com.netcracker.cloud.dbaas.client.entity.database.type.DatabaseType;

/**
 * Test postgresql database type.
 */
public class PostgresDBType extends DatabaseType<PostgresDBConnection, PostgresDatabase> {
    public static final PostgresDBType INSTANCE = new PostgresDBType();

    private PostgresDBType() {
        super("postgresql", PostgresDatabase.class);
    }
}
