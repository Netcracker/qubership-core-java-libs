package com.netcracker.cloud.dbaas.client.management;

import com.arangodb.ArangoCursorAsync;
import com.arangodb.ArangoDatabase;
import com.arangodb.ArangoDatabaseAsync;
import com.netcracker.cloud.dbaas.client.arangodb.entity.connection.ArangoConnection;
import com.netcracker.cloud.dbaas.client.arangodb.entity.database.type.ArangoDBType;
import com.netcracker.cloud.dbaas.client.management.classifier.DbaaSChainClassifierBuilder;
import com.netcracker.cloud.dbaas.client.management.classifier.ServiceDbaaSClassifierBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.netcracker.cloud.dbaas.client.arangodb.classifier.ArangoDBClassifierBuilder.DB_ID_CLASSIFIER_PROPERTY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class ArangoDatabaseProviderTest {

    private static final String DB_NAME_1 = "db-test-name-1";
    private static final String DB_NAME_2 = "db-test-name-2";
    private static ArangoDatabaseProvider arangoDatabaseProvider;
    private static DatabasePool databasePool;
    // The async "RETURN 42" probe result, read by the getOrCreateDatabase answer at invocation
    // time. Tests swap it (never-completing / failed) before calling provide() to drive the check.
    private static CompletableFuture<ArangoCursorAsync<Integer>> checkFuture;

    @BeforeEach
    public void setup() {
        databasePool = mock(DatabasePool.class);
        checkFuture = CompletableFuture.completedFuture(cursorAsyncReturning(42));
        Mockito.when(databasePool.getOrCreateDatabase(any(ArangoDBType.class), any(DbaasDbClassifier.class), any(DatabaseConfig.class))).thenAnswer(
                invocationOnMock -> {
                    DbaasDbClassifier dbaasDbClassifier = invocationOnMock.getArgument(1);
                    String dbName = (String) dbaasDbClassifier.asMap().get(DB_ID_CLASSIFIER_PROPERTY);
                    ArangoDatabase arangoDatabase = mock(ArangoDatabase.class);
                    Mockito.when(arangoDatabase.name()).thenReturn(dbName);
                    ArangoDatabaseAsync arangoDatabaseAsync = mock(ArangoDatabaseAsync.class);
                    Mockito.when(arangoDatabaseAsync.query(eq("RETURN 42"), eq(Integer.class))).thenReturn(checkFuture);
                    ArangoConnection arangoConnection = new ArangoConnection();
                    arangoConnection.setArangoDatabase(arangoDatabase);
                    arangoConnection.setArangoDatabaseAsync(arangoDatabaseAsync);
                    com.netcracker.cloud.dbaas.client.arangodb.entity.database.ArangoDatabase result = new com.netcracker.cloud.dbaas.client.arangodb.entity.database.ArangoDatabase();
                    result.setConnectionProperties(arangoConnection);
                    return result;
                }
        );
        DbaaSChainClassifierBuilder classifierBuilder = new ServiceDbaaSClassifierBuilder(null);
        arangoDatabaseProvider = new ArangoDatabaseProvider(databasePool, classifierBuilder, DatabaseConfig.builder().build());
    }

    @SuppressWarnings("unchecked")
    private static ArangoCursorAsync<Integer> cursorAsyncReturning(Integer value) {
        ArangoCursorAsync<Integer> cursor = mock(ArangoCursorAsync.class);
        when(cursor.getResult()).thenReturn(List.of(value));
        return cursor;
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
        ArangoDatabase firstDb = arangoDatabaseProvider.provide(DB_NAME_1, firstDatabaseConfig);
        ArangoDatabase secondDb = arangoDatabaseProvider.provide(DB_NAME_2, secondDatabaseConfig);
        Assertions.assertNotEquals(firstDb.name(), secondDb.name());
        // Check passes first try, so each physical instance is fetched exactly once.
        Mockito.verify(databasePool, times(1)).getOrCreateDatabase(any(ArangoDBType.class), any(), eq(firstDatabaseConfig));
        Mockito.verify(databasePool, times(1)).getOrCreateDatabase(any(ArangoDBType.class), any(), eq(secondDatabaseConfig));
    }

    @Test
    void testCheckConnection_Timeout_TreatedAsFailure() {
        checkFuture = new CompletableFuture<>(); // never completes -> get(timeout) times out
        ArangoDatabaseProvider provider = new ArangoDatabaseProvider(
                databasePool, new ServiceDbaaSClassifierBuilder(null), DatabaseConfig.builder().build(), 0, 0L, 100L);
        // timeout counts as a failed check; retries=0 -> exhaustion -> throw (decision 11)
        Assertions.assertThrows(IllegalStateException.class, () -> provider.provide(DB_NAME_1));
        // initial + reconnect
        verify(databasePool, times(2)).getOrCreateDatabase(any(ArangoDBType.class), any(), any(DatabaseConfig.class));
    }

    @Test
    void testCheckConnection_Interrupted_Rethrows() {
        checkFuture = new CompletableFuture<>(); // never completes
        ArangoDatabaseProvider provider = new ArangoDatabaseProvider(
                databasePool, new ServiceDbaaSClassifierBuilder(null), DatabaseConfig.builder().build(), 0, 0L, 60_000L);

        Thread.currentThread().interrupt(); // caller interrupted -> future.get() aborts with InterruptedException
        // decision 13: the base provider restores the flag AND rethrows (abort, don't retry)
        Assertions.assertThrows(RuntimeException.class, () -> provider.provide(DB_NAME_1));
        // flag is re-set by the InterruptedException branch; verify + clear so it can't leak
        Assertions.assertTrue(Thread.interrupted());
        // aborted during the very first check -> only the initial getOrCreate happened
        verify(databasePool, times(1))
                .getOrCreateDatabase(any(ArangoDBType.class), any(), any(DatabaseConfig.class));
    }

    @Test
    void testRetryAttempts() {
        int retries = 5;
        ArangoDatabaseProvider databaseProvider =
                new ArangoDatabaseProvider(databasePool, new ServiceDbaaSClassifierBuilder(null), DatabaseConfig.builder().build(), retries, 1L, 60_000L);
        DatabaseConfig databaseConfig = DatabaseConfig.builder()
                .physicalDatabaseId("retry-db-id-1")
                .userRole("admin")
                .dbNamePrefix("db_prefix_retry")
                .backupDisabled(true)
                .build();
        checkFuture = CompletableFuture.failedFuture(new RuntimeException("check failed"));
        Assertions.assertThrows(IllegalStateException.class, () -> databaseProvider.provide(DB_NAME_1, databaseConfig));
        int initialInvocationNumber = 1;
        int reconnectInvocationNumber = 1;
        Mockito.verify(databasePool, times(initialInvocationNumber + reconnectInvocationNumber + retries))
                .getOrCreateDatabase(any(ArangoDBType.class), any(), eq(databaseConfig));
    }
}
