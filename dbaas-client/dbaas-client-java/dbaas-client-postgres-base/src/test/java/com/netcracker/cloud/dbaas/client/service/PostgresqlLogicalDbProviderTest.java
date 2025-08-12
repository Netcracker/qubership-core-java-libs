package com.netcracker.cloud.dbaas.client.service;

import com.netcracker.cloud.dbaas.client.entity.connection.PostgresDBConnection;
import com.netcracker.cloud.dbaas.client.entity.database.PostgresDatabase;
import com.netcracker.cloud.dbaas.client.management.DatabaseConfig;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.SortedMap;

import static com.netcracker.cloud.dbaas.client.DbaasConst.*;
import static com.netcracker.cloud.dbaas.client.service.PostgresqlLogicalDbProviderImpl.PASSWORD;
import static com.netcracker.cloud.dbaas.client.service.PostgresqlLogicalDbProviderImpl.URL;
import static com.netcracker.cloud.dbaas.client.service.PostgresqlLogicalDbProviderImpl.USERNAME;

public class PostgresqlLogicalDbProviderTest {
    @Test
    public void checkPostgresLogicalDbProvider() {
        PostgresqlLogicalDbProvider logicalDbProvider = new PostgresqlLogicalDbProviderImpl();

        PostgresDatabase provide = logicalDbProvider.provide(Collections.emptySortedMap(), DatabaseConfig.builder().build(), "test-ns");
        PostgresDBConnection connectionProperties = provide.getConnectionProperties();

        Assertions.assertNotNull(connectionProperties);
        Assertions.assertEquals("my-pg", provide.getName());
        Assertions.assertEquals(URL, connectionProperties.getUrl());
        Assertions.assertEquals(PASSWORD, connectionProperties.getPassword());
        Assertions.assertEquals(USERNAME, connectionProperties.getUsername());
        Assertions.assertEquals(ADMIN_ROLE, connectionProperties.getRole());
    }

    @Test
    public void checkPostgresSupportedDatabaseType() {
        PostgresqlLogicalDbProvider logicalDbProvider = new PostgresqlLogicalDbProviderImpl();
        Assertions.assertEquals(PostgresDatabase.class, logicalDbProvider.getSupportedDatabaseType());

    }

    @Test
    public void testDatabaseNameCanBeResolvedFromConnection() {
        PostgresqlLogicalDbProvider logicalDbProvider = new PostgresqlLogicalDbProvider() {
            @Override
            public PostgresConnectionProperty provideConnectionProperty(SortedMap<String, Object> classifier, DatabaseConfig params) {
                return new PostgresConnectionProperty(
                        "jdbc:postgresql://127.0.0.1:5432/test-db-name",
                        USERNAME,
                        PASSWORD,
                        ADMIN_ROLE
                );
            }
        };

        PostgresDatabase db = logicalDbProvider.provide(Collections.emptySortedMap(), DatabaseConfig.builder().build(), "test-ns");
        Assertions.assertEquals("test-db-name", db.getName());
    }
}
