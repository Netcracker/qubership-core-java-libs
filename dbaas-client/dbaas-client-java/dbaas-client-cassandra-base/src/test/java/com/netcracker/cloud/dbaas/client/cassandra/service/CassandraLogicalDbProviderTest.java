package com.netcracker.cloud.dbaas.client.cassandra.service;

import com.netcracker.cloud.dbaas.client.cassandra.entity.connection.CassandraDBConnection;
import com.netcracker.cloud.dbaas.client.cassandra.entity.database.CassandraDatabase;
import com.netcracker.cloud.dbaas.client.management.DatabaseConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static com.netcracker.cloud.dbaas.client.DbaasConst.ADMIN_ROLE;
import static com.netcracker.cloud.dbaas.client.cassandra.service.CassandraLogicalDbProviderImpl.*;

public class CassandraLogicalDbProviderTest {

    @Test
    public void checkCassandraLogicalDbProvider() {
        CassandraLogicalDbProviderImpl logicalDbProvider = new CassandraLogicalDbProviderImpl();

        CassandraDatabase provide = logicalDbProvider.provide(Collections.emptySortedMap(), DatabaseConfig.builder().build(), "test-ns");
        CassandraDBConnection connectionProperties = provide.getConnectionProperties();

       Assertions.assertNotNull(connectionProperties);
       Assertions.assertEquals("test-cassandra", provide.getName());
       Assertions.assertEquals(URL, connectionProperties.getUrl());
       Assertions.assertEquals(USERNAME, connectionProperties.getUsername());
       Assertions.assertEquals(PWD, connectionProperties.getPassword());
       Assertions.assertEquals(CONTRACT_POINTS, connectionProperties.getContactPoints());
       Assertions.assertEquals(KEYSPACE, connectionProperties.getKeyspace());
       Assertions.assertEquals(PORT, connectionProperties.getPort());
       Assertions.assertEquals(ADMIN_ROLE, connectionProperties.getRole());
    }

    @Test
    public void checkCassandraSupportedDatabaseType() {
        CassandraLogicalDbProviderImpl logicalDbProvider = new CassandraLogicalDbProviderImpl();
       Assertions.assertEquals(CassandraDatabase.class, logicalDbProvider.getSupportedDatabaseType());

    }

}
