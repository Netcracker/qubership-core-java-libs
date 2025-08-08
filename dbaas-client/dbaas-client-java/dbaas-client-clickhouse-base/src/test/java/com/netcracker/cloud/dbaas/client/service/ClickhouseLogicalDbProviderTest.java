package com.netcracker.cloud.dbaas.client.service;

import com.netcracker.cloud.dbaas.client.entity.connection.ClickhouseConnection;
import com.netcracker.cloud.dbaas.client.entity.database.ClickhouseDatabase;
import com.netcracker.cloud.dbaas.client.management.DatabaseConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.SortedMap;

import static com.netcracker.cloud.dbaas.client.DbaasConst.ADMIN_ROLE;
import static com.netcracker.cloud.dbaas.client.service.ClickhouseLogicalDbProviderImpl.*;

class ClickhouseLogicalDbProviderTest {
    @Test
    void checkClickhouseLogicalDbProvider() {
        ClickhouseLogicalDbProvider logicalDbProvider = new ClickhouseLogicalDbProviderImpl();

        ClickhouseDatabase provide = logicalDbProvider.provide(Collections.emptySortedMap(), DatabaseConfig.builder().build(), "test-ns");
        ClickhouseConnection connectionProperties = provide.getConnectionProperties();

        Assertions.assertNotNull(connectionProperties);
        Assertions.assertEquals("my-cl", provide.getName());
        Assertions.assertEquals(URL, connectionProperties.getUrl());
        Assertions.assertEquals(PASSWORD, connectionProperties.getPassword());
        Assertions.assertEquals(USERNAME, connectionProperties.getUsername());
        Assertions.assertEquals(ADMIN_ROLE, connectionProperties.getRole());
    }

    @Test
    void checkClickhouseSupportedDatabaseType() {
        ClickhouseLogicalDbProvider logicalDbProvider = new ClickhouseLogicalDbProviderImpl();
        Assertions.assertEquals(ClickhouseDatabase.class, logicalDbProvider.getSupportedDatabaseType());

    }

    @Test
    void testDatabaseNameCanBeResolvedFromConnection() {
        ClickhouseLogicalDbProvider logicalDbProvider = new ClickhouseLogicalDbProvider() {
            @Override
            public ClickhouseConnectionProperty provideConnectionProperty(SortedMap<String, Object> classifier, DatabaseConfig params) {
                return new ClickhouseConnectionProperty(
                        "jdbc:clickhouse://127.0.0.1:5432/test-db-name",
                        USERNAME,
                        PASSWORD,
                        ADMIN_ROLE
                );
            }
        };

        ClickhouseDatabase db = logicalDbProvider.provide(Collections.emptySortedMap(), DatabaseConfig.builder().build(), "test-ns");
        Assertions.assertEquals("test-db-name", db.getName());
    }
}
