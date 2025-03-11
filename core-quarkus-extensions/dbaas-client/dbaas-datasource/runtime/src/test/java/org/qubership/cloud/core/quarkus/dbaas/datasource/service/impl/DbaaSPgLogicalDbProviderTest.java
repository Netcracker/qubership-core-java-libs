package org.qubership.cloud.core.quarkus.dbaas.datasource.service.impl;

import org.qubership.cloud.dbaas.client.DbaaSClientOkHttpImpl;
import org.qubership.cloud.dbaas.client.DbaasClient;
import org.qubership.cloud.dbaas.client.entity.connection.PostgresDBConnection;
import org.qubership.cloud.dbaas.client.entity.database.PostgresDatabase;
import org.qubership.cloud.dbaas.client.entity.database.type.PostgresDBType;
import org.qubership.cloud.dbaas.client.management.DatabaseConfig;
import org.junit.jupiter.api.Test;

import java.util.SortedMap;
import java.util.TreeMap;

import static org.qubership.cloud.core.quarkus.dbaas.datasource.CommonTestUtils.TEST_NAMESPACE;
import static org.qubership.cloud.dbaas.client.DbaasConst.SCOPE;
import static org.qubership.cloud.dbaas.client.DbaasConst.SERVICE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DbaaSPgLogicalDbProviderTest {

    @Test
    public void orderMustBeMaximum() {
        DbaaSPgLogicalDbProvider dbaaSPgLogicalDbProvider = new DbaaSPgLogicalDbProvider(null);
        assertEquals(Integer.MAX_VALUE, dbaaSPgLogicalDbProvider.order());
    }

    @Test
    public void dbaasPgProviderMustReturnPgLogicalDb() {
        TreeMap<String, Object> classifier = new TreeMap<>();
        classifier.put(SCOPE, SERVICE);
        classifier.put("microserviceName", "test-ms");
        classifier.put("namespace", TEST_NAMESPACE);

        PostgresDatabase expectedPostgresDatabase = new PostgresDatabase();
        expectedPostgresDatabase.setClassifier((SortedMap<String, Object>) classifier.clone()); // clone because after we do assertion
        expectedPostgresDatabase.setNamespace(TEST_NAMESPACE);
        String dbName = "dbaas_123456789";
        expectedPostgresDatabase.setName(dbName);
        expectedPostgresDatabase.setConnectionProperties(new PostgresDBConnection("pg_url", "username", "password", "admin"));


        DbaasClient dbaasClient = mock(DbaaSClientOkHttpImpl.class);
        DatabaseConfig databaseConfig = DatabaseConfig.builder().userRole("admin").build();
        when(dbaasClient.getOrCreateDatabase(PostgresDBType.INSTANCE, TEST_NAMESPACE, classifier, databaseConfig))
                .thenReturn(expectedPostgresDatabase);
        when(dbaasClient.getConnection(PostgresDBType.INSTANCE, TEST_NAMESPACE, databaseConfig.getUserRole(), classifier))
                .thenReturn(expectedPostgresDatabase.getConnectionProperties());

        DbaaSPgLogicalDbProvider dbaaSPgLogicalDbProvider = new DbaaSPgLogicalDbProvider(dbaasClient);
        PostgresDatabase actualPgDatabase = dbaaSPgLogicalDbProvider.provide(classifier, databaseConfig, TEST_NAMESPACE);

        assertEquals(TEST_NAMESPACE, actualPgDatabase.getNamespace());
        assertEquals(classifier, actualPgDatabase.getClassifier());
        assertEquals(dbName, actualPgDatabase.getName());
        assertEquals("pg_url", actualPgDatabase.getConnectionProperties().getUrl());
        assertEquals("password", actualPgDatabase.getConnectionProperties().getPassword());
        assertEquals("username", actualPgDatabase.getConnectionProperties().getUsername());
    }

}