package com.netcracker.cloud.dbaas.client.management;

import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDatabase;
import com.netcracker.cloud.dbaas.client.arangodb.entity.connection.ArangoConnection;
import com.netcracker.cloud.dbaas.client.arangodb.entity.database.type.ArangoDBType;
import com.netcracker.cloud.dbaas.client.management.classifier.DbaaSChainClassifierBuilder;
import com.netcracker.cloud.dbaas.client.management.classifier.ServiceDbaaSClassifierBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static com.netcracker.cloud.dbaas.client.arangodb.classifier.ArangoDBClassifierBuilder.DB_ID_CLASSIFIER_PROPERTY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;

public class ArangoDatabaseProviderTest {

    private static final String DB_NAME_1 = "db-test-name-1";
    private static final String DB_NAME_2 = "db-test-name-2";
    private static ArangoDatabaseProvider arangoDatabaseProvider;
    private static DatabasePool databasePool;
    private static ArangoCursor<Integer> cursor;

    @BeforeEach
    public void setup() {
        databasePool = Mockito.mock(DatabasePool.class);
        cursor = Mockito.mock(ArangoCursor.class);
        Mockito.when(cursor.next()).thenReturn(42);
        Mockito.when(databasePool.getOrCreateDatabase(any(ArangoDBType.class), any(DbaasDbClassifier.class), any(DatabaseConfig.class))).thenAnswer(
                invocationOnMock -> {
                    DbaasDbClassifier dbaasDbClassifier = invocationOnMock.getArgument(1);
                    String dbName = (String) dbaasDbClassifier.asMap().get(DB_ID_CLASSIFIER_PROPERTY);
                    ArangoDatabase arangoDatabase = Mockito.mock(ArangoDatabase.class);
                    Mockito.when(arangoDatabase.name()).thenReturn(dbName);
                    ArangoConnection arangoConnection = new ArangoConnection();
                    arangoConnection.setArangoDatabase(arangoDatabase);
                    Mockito.when(arangoDatabase.query(any(), eq(Integer.class))).thenReturn(cursor);
                    com.netcracker.cloud.dbaas.client.arangodb.entity.database.ArangoDatabase result = new com.netcracker.cloud.dbaas.client.arangodb.entity.database.ArangoDatabase();
                    result.setConnectionProperties(arangoConnection);
                    return result;
                }
        );
        DbaaSChainClassifierBuilder classifierBuilder = new ServiceDbaaSClassifierBuilder(null);
        arangoDatabaseProvider = new ArangoDatabaseProvider(databasePool, classifierBuilder, DatabaseConfig.builder().build());
    }

    @Test
    void testGetMultipleDbs() {
        ArangoDatabase firstDb = arangoDatabaseProvider.provide(DB_NAME_1);
        ArangoDatabase secondDb = arangoDatabaseProvider.provide(DB_NAME_2);
        Assertions.assertNotEquals(firstDb.name(), secondDb.name());
    }

    @Test
    void testGetSameDb() {
        ArangoDatabase firstDb = arangoDatabaseProvider.provide(DB_NAME_1);
        ArangoDatabase secondDb = arangoDatabaseProvider.provide(DB_NAME_1);
        Assertions.assertEquals(firstDb.name(), secondDb.name());
    }

    @Test
    void testGetDefaultDb() {
        ArangoDatabase defaultDb = arangoDatabaseProvider.provide();
        Assertions.assertNotNull(defaultDb);
    }

    @Test
    void testGetMultipleDbsOnDifferentPhysicalDatabaseInstance() {
        DatabaseConfig firstDatabaseConfig = DatabaseConfig.builder()
                .physicalDatabaseId("physical-db-id-1")
                .userRole("admin")
                .dbNamePrefix("db_prefix_1")
                .backupDisabled(true)
                .build();
        DatabaseConfig secondDatabaseConfig = DatabaseConfig.builder()
                .physicalDatabaseId("physical-db-id-2")
                .userRole("rw")
                .dbNamePrefix("db_prefix_2")
                .backupDisabled(true)
                .build();
        Mockito.when(cursor.next()).thenThrow(new RuntimeException());
        ArangoDatabase firstDb = arangoDatabaseProvider.provide(DB_NAME_1, firstDatabaseConfig);
        ArangoDatabase secondDb = arangoDatabaseProvider.provide(DB_NAME_2, secondDatabaseConfig);
        Assertions.assertNotEquals(firstDb.name(), secondDb.name());
        Mockito.verify(databasePool, times(2)).getOrCreateDatabase(any(ArangoDBType.class), any(), eq(firstDatabaseConfig));
        Mockito.verify(databasePool, times(2)).getOrCreateDatabase(any(ArangoDBType.class), any(), eq(secondDatabaseConfig));
    }

    @Test
    void testRetryAttempts() {
        int retries = 5;
        ArangoDatabaseProvider databaseProvider =
                new ArangoDatabaseProvider(databasePool, new ServiceDbaaSClassifierBuilder(null), DatabaseConfig.builder().build(), retries, 1L);
        DatabaseConfig databaseConfig = DatabaseConfig.builder()
                .physicalDatabaseId("retry-db-id-1")
                .userRole("admin")
                .dbNamePrefix("db_prefix_retry")
                .backupDisabled(true)
                .build();
        Mockito.when(cursor.next()).thenThrow(new RuntimeException());
        databaseProvider.provide(DB_NAME_1, databaseConfig);
        int initialInvocationNumber = 1;
        int reconnectInvocationNumber = 1;
        Mockito.verify(databasePool, times(initialInvocationNumber + reconnectInvocationNumber + retries))
                .getOrCreateDatabase(any(ArangoDBType.class), any(), eq(databaseConfig));
    }
}
