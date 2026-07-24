package com.netcracker.cloud.dbaas.client.management;

import com.arangodb.ArangoCursorAsync;
import com.arangodb.ArangoDB;
import com.arangodb.ArangoDBAsync;
import com.arangodb.ArangoDatabase;
import com.arangodb.ArangoDatabaseAsync;
import com.netcracker.cloud.dbaas.client.arangodb.entity.connection.ArangoConnection;
import com.netcracker.cloud.dbaas.client.arangodb.entity.database.type.ArangoDBType;
import com.netcracker.cloud.dbaas.client.management.classifier.DbaaSChainClassifierBuilder;
import com.netcracker.cloud.dbaas.client.management.classifier.ServiceDbaaSClassifierBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static com.netcracker.cloud.dbaas.client.arangodb.classifier.ArangoDBClassifierBuilder.DB_ID_CLASSIFIER_PROPERTY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ArangoDatabaseProviderTest {

    private static final String DB_NAME_1 = "db-test-name-1";
    private static final String DB_NAME_2 = "db-test-name-2";
    private static ArangoDatabaseProvider arangoDatabaseProvider;
    private static DatabasePool databasePool;
    // The async "RETURN 42" probe result, read by the getOrCreateDatabase answer at invocation
    // time. Tests swap it (never-completing / failed) before calling provide() to drive the check.
    private static CompletableFuture<ArangoCursorAsync<Integer>> checkFuture;

    @BeforeEach
    void setup() {
        databasePool = mock(DatabasePool.class);
        checkFuture = CompletableFuture.completedFuture(cursorAsyncReturning(42));
        when(databasePool.getOrCreateDatabase(any(ArangoDBType.class), any(DbaasDbClassifier.class), any(DatabaseConfig.class))).thenAnswer(
                invocationOnMock -> {
                    DbaasDbClassifier dbaasDbClassifier = invocationOnMock.getArgument(1);
                    String dbName = (String) dbaasDbClassifier.asMap().get(DB_ID_CLASSIFIER_PROPERTY);
                    ArangoConnection arangoConnection = mockArangoConnection(dbName, checkFuture);
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

    // The probe now derives the async handle as arangoDatabase.arango().async().db(dbName)
    // (see ArangoDatabaseProvider.checkConnection), so the sync ArangoDatabase mock has to wire
    // that whole chain instead of a directly-settable async field.
    private static ArangoConnection mockArangoConnection(String dbName, CompletableFuture<ArangoCursorAsync<Integer>> future) {
        ArangoDatabaseAsync arangoDatabaseAsync = mock(ArangoDatabaseAsync.class);
        when(arangoDatabaseAsync.query("RETURN 42", Integer.class)).thenReturn(future);
        ArangoDBAsync arangoDBAsync = mock(ArangoDBAsync.class);
        when(arangoDBAsync.db(dbName)).thenReturn(arangoDatabaseAsync);
        ArangoDB arangoDB = mock(ArangoDB.class);
        when(arangoDB.async()).thenReturn(arangoDBAsync);
        ArangoDatabase arangoDatabase = mock(ArangoDatabase.class);
        when(arangoDatabase.name()).thenReturn(dbName);
        when(arangoDatabase.arango()).thenReturn(arangoDB);
        ArangoConnection arangoConnection = new ArangoConnection();
        arangoConnection.setDbName(dbName);
        arangoConnection.setArangoDatabase(arangoDatabase);
        return arangoConnection;
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
        verify(databasePool, times(1)).getOrCreateDatabase(any(ArangoDBType.class), any(), eq(firstDatabaseConfig));
        verify(databasePool, times(1)).getOrCreateDatabase(any(ArangoDBType.class), any(), eq(secondDatabaseConfig));
    }

    @Test
    void testCheckConnection_Timeout_TreatedAsFailure() {
        checkFuture = new CompletableFuture<>(); // never completes -> get(timeout) times out
        // retries=1, retryDelay=1L: small but non-zero so the test still exercises a real
        // retry + exhaustion without waiting on the production retry delay.
        ArangoDatabaseProvider provider = new ArangoDatabaseProvider(
                databasePool, new ServiceDbaaSClassifierBuilder(null), DatabaseConfig.builder().build(), 1, 1L, 100L);
        // timeout counts as a failed check; retries exhausted -> throw (decision 11)
        Assertions.assertThrows(IllegalStateException.class, () -> provider.provide(DB_NAME_1));
        // initial + reconnect + 1 retry recreate; every one of these 3 connections is checked
        // (including the last retry recreate) before exhaustion is declared
        verify(databasePool, times(3)).getOrCreateDatabase(any(ArangoDBType.class), any(), any(DatabaseConfig.class));
    }

    @Test
    void testCheckConnection_LastRetryRecreateHealthy_ReturnsIt() {
        // Reproduces the bug where the connection recreated right before exhaustion was thrown
        // away unchecked: with retries=1 there are 3 getOrCreateDatabase calls (initial, reconnect,
        // 1 retry recreate). Only the 3rd (the final retry recreate) is healthy here; provide()
        // must check it and return it instead of throwing.
        AtomicInteger callCount = new AtomicInteger();
        when(databasePool.getOrCreateDatabase(any(ArangoDBType.class), any(DbaasDbClassifier.class), any(DatabaseConfig.class))).thenAnswer(
                invocationOnMock -> {
                    DbaasDbClassifier dbaasDbClassifier = invocationOnMock.getArgument(1);
                    String dbName = (String) dbaasDbClassifier.asMap().get(DB_ID_CLASSIFIER_PROPERTY);
                    boolean healthy = callCount.incrementAndGet() == 3;
                    CompletableFuture<ArangoCursorAsync<Integer>> future = healthy
                            ? CompletableFuture.completedFuture(cursorAsyncReturning(42))
                            : CompletableFuture.failedFuture(new RuntimeException("check failed"));
                    ArangoConnection arangoConnection = mockArangoConnection(dbName, future);
                    com.netcracker.cloud.dbaas.client.arangodb.entity.database.ArangoDatabase result = new com.netcracker.cloud.dbaas.client.arangodb.entity.database.ArangoDatabase();
                    result.setConnectionProperties(arangoConnection);
                    return result;
                }
        );

        ArangoDatabaseProvider provider = new ArangoDatabaseProvider(
                databasePool, new ServiceDbaaSClassifierBuilder(null), DatabaseConfig.builder().build(), 1, 1L, 100L);

        ArangoDatabase db = provider.provide(DB_NAME_1);
        Assertions.assertNotNull(db);
        // no wasted 4th recreate after the successful check
        verify(databasePool, times(3)).getOrCreateDatabase(any(ArangoDBType.class), any(), any(DatabaseConfig.class));
    }

    @Test
    void testCheckConnection_Interrupted_TreatedAsFailure() {
        checkFuture = new CompletableFuture<>(); // never completes -> get(timeout) aborts with InterruptedException
        ArangoDatabaseProvider provider = new ArangoDatabaseProvider(
                databasePool, new ServiceDbaaSClassifierBuilder(null), DatabaseConfig.builder().build(), 0, 0L, 60_000L);

        Thread.currentThread().interrupt(); // caller interrupted before the check even starts
        // interrupt is treated the same as any other failed check, matching DbaasArangoTemplate
        // and pre-existing (pre-PR) behavior -> exhausts the single recreate-and-check attempt, then throws
        Assertions.assertThrows(IllegalStateException.class, () -> provider.provide(DB_NAME_1));
        // flag is restored by the InterruptedException branch; verify + clear so it can't leak
        Assertions.assertTrue(Thread.interrupted());
        // initial + a single recreate-and-check attempt, both short-circuited by the still-set
        // interrupt flag (Future.get() throws immediately when the calling thread is already interrupted)
        verify(databasePool, times(2))
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
        verify(databasePool, times(initialInvocationNumber + reconnectInvocationNumber + retries))
                .getOrCreateDatabase(any(ArangoDBType.class), any(), eq(databaseConfig));
    }

    @Test
    void testRetries_ZeroMeansZero_NoFallbackToDefault() {
        // The 6-arg constructor takes retries/retryDelay literally: 0 means "no retries".
        ArangoDatabaseProvider provider = new ArangoDatabaseProvider(
                databasePool, new ServiceDbaaSClassifierBuilder(null), DatabaseConfig.builder().build(), 0, 0L, 100L);
        checkFuture = CompletableFuture.failedFuture(new RuntimeException("check failed"));
        Assertions.assertThrows(IllegalStateException.class, () -> provider.provide(DB_NAME_1));
        // initial + a single recreate-and-check attempt, no retries beyond that
        verify(databasePool, times(2)).getOrCreateDatabase(any(ArangoDBType.class), any(), any(DatabaseConfig.class));
    }
}
