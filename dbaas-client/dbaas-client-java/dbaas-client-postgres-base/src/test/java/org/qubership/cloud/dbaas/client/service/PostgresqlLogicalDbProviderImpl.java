package org.qubership.cloud.dbaas.client.service;

import org.qubership.cloud.dbaas.client.entity.database.PostgresDatabase;
import org.qubership.cloud.dbaas.client.management.DatabaseConfig;

import java.util.SortedMap;

import static org.qubership.cloud.dbaas.client.DbaasConst.*;

public class PostgresqlLogicalDbProviderImpl extends PostgresqlLogicalDbProvider {
    static final String URL = "url";
    static final String USERNAME = "username";
    static final String PASSWORD = "pwd";

    @Override
    public PostgresConnectionProperty provideConnectionProperty(SortedMap<String, Object> classifier, DatabaseConfig params) {
        return new PostgresConnectionProperty(
                URL,
                USERNAME,
                PASSWORD,
                ADMIN_ROLE
        );
    }

    @Override
    public void provideDatabaseInfo(PostgresDatabase database) {
        database.setName("my-pg");
    }
}
