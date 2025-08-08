package com.netcracker.cloud.dbaas.client.test;

import com.netcracker.cloud.dbaas.client.DbaasClient;
import com.netcracker.cloud.dbaas.client.entity.database.AbstractDatabase;
import com.netcracker.cloud.dbaas.client.entity.database.type.DatabaseType;
import com.netcracker.cloud.dbaas.client.management.*;
import com.netcracker.cloud.dbaas.client.management.classifier.ServiceDbaaSClassifierBuilder;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

class DatabasePoolTestUtilTest {

    private static final String TEST_MS_NAME = "test-ms";
    private static final String TEST_NAMESPACE = "test-namespace";

    @Test
    void clearDatabasePoolCacheTest() {
        DbaasClient dbaasClient = Mockito.mock(DbaasClient.class);
        DatabasePool databasePool = new DatabasePool(dbaasClient, TEST_MS_NAME, TEST_NAMESPACE, Collections.singletonList(Mockito.mock(PostConnectProcessor.class)), Mockito.mock(DatabaseDefinitionHandler.class));
        DatabaseType databaseType = new DatabaseType("dbType", AbstractDatabase.class);
        AbstractDatabase abstractDatabase = Mockito.mock(AbstractDatabase.class);
        when(abstractDatabase.getName()).thenReturn("test-db");

        Mockito.when(dbaasClient.getOrCreateDatabase(
                eq(databaseType),
                eq(TEST_NAMESPACE),
                any(),
                any(DatabaseConfig.class))).thenReturn(abstractDatabase);

        DbaasDbClassifier dbClassifier = new ServiceDbaaSClassifierBuilder().build();
        databasePool.getOrCreateDatabase(databaseType, dbClassifier);
        databasePool.getOrCreateDatabase(databaseType, dbClassifier);  // return from cache

        Mockito.verify(dbaasClient, times(1)).getOrCreateDatabase(
                eq(databaseType),
                eq(TEST_NAMESPACE),
                anyMap(),
                any(DatabaseConfig.class));

        DatabasePoolTestUtils databasePoolTestUtil = new DatabasePoolTestUtils(databasePool);
        databasePoolTestUtil.clearCache();

        databasePool.getOrCreateDatabase(databaseType, dbClassifier);

        Mockito.verify(dbaasClient, times(2)).getOrCreateDatabase(
                eq(databaseType),
                eq(TEST_NAMESPACE),
                anyMap(),
                any(DatabaseConfig.class));
    }
}
