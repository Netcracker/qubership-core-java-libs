package org.qubership.cloud.dbaas.client;

import com.datastax.oss.driver.api.core.AllNodesFailedException;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PrepareRequest;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.core.cql.Statement;
import com.datastax.oss.driver.internal.core.cql.DefaultPrepareRequest;
import org.qubership.cloud.dbaas.client.cassandra.entity.connection.CassandraDBConnection;
import org.qubership.cloud.dbaas.client.cassandra.entity.database.CassandraDatabase;
import org.qubership.cloud.dbaas.client.cassandra.entity.database.type.CassandraDBType;
import org.qubership.cloud.dbaas.client.management.DatabaseConfig;
import org.qubership.cloud.dbaas.client.management.DatabasePool;
import org.qubership.cloud.dbaas.client.management.classifier.DbaaSClassifierBuilder;
import org.qubership.cloud.dbaas.client.management.classifier.ServiceDbaaSClassifierBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CassandraDbaaSSessionProxyTest {
    private final DbaaSClassifierBuilder classifierBuilder = new ServiceDbaaSClassifierBuilder(null);

    private final DatabasePool databasePool = Mockito.mock(DatabasePool.class);
    private final CassandraDatabase cassandraDatabase = Mockito.mock(CassandraDatabase.class);
    private final CassandraDBConnection cassandraDBConnection = Mockito.mock(CassandraDBConnection.class);
    private final CqlSession wrappedSession = Mockito.mock(CqlSession.class);

    private DatabaseConfig databaseConfig = DatabaseConfig.builder().userRole("admin").build();

    private final CassandraDbaaSSessionProxy sessionProxy = new CassandraDbaaSSessionProxy(databasePool,
            classifierBuilder, databaseConfig);

    @BeforeEach
    public void setUp() throws Exception {
        Mockito.reset(databasePool, wrappedSession, cassandraDatabase, cassandraDBConnection, wrappedSession);

        Mockito.when(
                databasePool.getOrCreateDatabase(eq(CassandraDBType.INSTANCE), eq(classifierBuilder.build()), any()))
                .thenReturn(cassandraDatabase);
        Mockito.when(cassandraDatabase.getConnectionProperties()).thenReturn(cassandraDBConnection);
        Mockito.when(cassandraDBConnection.getSession()).thenReturn(wrappedSession);

        Mockito.when(wrappedSession.execute(any(Statement.class))).thenReturn(mock(ResultSet.class));
        Mockito.when(wrappedSession.execute(any(Statement.class), eq(Statement.SYNC)))
                .thenReturn(mock(ResultSet.class));
        Mockito.when(wrappedSession.execute(any(Statement.class), eq(Statement.ASYNC)))
                .thenReturn(mock(CompletionStage.class));
        Mockito.when(wrappedSession.execute(any(DefaultPrepareRequest.class), eq(PrepareRequest.SYNC)))
                .thenReturn(mock(PreparedStatement.class));
        Mockito.when(wrappedSession.execute(any(DefaultPrepareRequest.class), eq(PrepareRequest.ASYNC)))
                .thenReturn(mock(CompletionStage.class));

        CompletionStage completionStage = mock(CompletionStage.class);
        Mockito.when(wrappedSession.closeFuture()).thenReturn(completionStage);
        Mockito.when(wrappedSession.closeAsync()).thenReturn(completionStage);

        CompletableFuture completableFuture = mock(CompletableFuture.class);
        Mockito.when(completionStage.toCompletableFuture()).thenReturn(completableFuture);
        Mockito.when(completableFuture.toCompletableFuture()).thenReturn(mock(CompletableFuture.class));
    }

    @Test
    public void getLoggedKeyspace() {
        sessionProxy.getKeyspace();
        Mockito.verify(wrappedSession, times(1)).getKeyspace();
    }

    @Test
    public void execute() {
        final String testQuery = "select * from test_table";
        sessionProxy.execute(testQuery);
        Mockito.verify(wrappedSession, times(1)).execute(any(Statement.class), eq(Statement.SYNC));
    }

    @Test
    public void testExecute() {
        final Statement<SimpleStatement> statement = SimpleStatement.builder("select * from test_table").build();
        sessionProxy.execute(statement);
        Mockito.verify(wrappedSession, times(1)).execute(any(Statement.class), eq(Statement.SYNC));
    }

    @Test
    public void testExecute1() {
        final String testQuery = "select * from test_table";
        final String testParam1 = "param1";
        final String testParam2 = "param2";
        sessionProxy.execute(testQuery, testParam1, testParam2);
        Mockito.verify(wrappedSession, times(1)).execute(any(Statement.class), eq(Statement.SYNC));
    }

    @Test
    public void testExecute2() {
        final String testQuery = "select * from test_table";
        final Map<String, Object> params = new HashMap<>(2);
        params.put("key1", "val1");
        params.put("key2", "val2");
        sessionProxy.execute(testQuery, params);
        Mockito.verify(wrappedSession, times(1)).execute(any(Statement.class), eq(Statement.SYNC));
    }

    @Test
    public void testExecute3() {
        Mockito.when(wrappedSession.execute(any(Statement.class), eq(Statement.SYNC)))
                .thenThrow(new IllegalStateException(
                        "Tried to execute unprepared query XXXXXXXX but we don't have the data to reprepare it"))
                .thenReturn(mock(ResultSet.class));

        BoundStatement bst = mock(BoundStatement.class);
        PreparedStatement pst = mock(PreparedStatement.class);
        Mockito.when(bst.getPreparedStatement()).thenReturn(pst);
        Mockito.when(pst.getQuery()).thenReturn("select *");

        sessionProxy.execute(bst);
        Mockito.verify(wrappedSession, times(2)).execute(any(BoundStatement.class), eq(Statement.SYNC));
        Mockito.verify(wrappedSession, times(1)).prepare(any(String.class));
    }

    @Test
    public void executeAsync() {
        final String testQuery = "select * from test_table";
        sessionProxy.executeAsync(testQuery);
        Mockito.verify(wrappedSession, times(1)).execute(any(Statement.class), eq(Statement.ASYNC));
    }

    @Test
    public void testExecuteAsync() {
        final Statement<SimpleStatement> statement = SimpleStatement.builder("select * from test_table").build();
        sessionProxy.executeAsync(statement);
        Mockito.verify(wrappedSession, times(1)).execute(any(Statement.class), eq(Statement.ASYNC));
    }

    @Test
    public void testExecuteAsync1() {
        final String testQuery = "select * from test_table";
        final String testParam1 = "param1";
        final String testParam2 = "param2";
        sessionProxy.executeAsync(testQuery, testParam1, testParam2);
        Mockito.verify(wrappedSession, times(1)).execute(any(Statement.class), eq(Statement.ASYNC));
    }

    @Test
    public void testExecuteAsync2() {
        final String testQuery = "select * from test_table";
        final Map<String, Object> params = new HashMap<>(2);
        params.put("key1", "val1");
        params.put("key2", "val2");
        sessionProxy.executeAsync(testQuery, params);
        Mockito.verify(wrappedSession, times(1)).execute(any(Statement.class), eq(Statement.ASYNC));
    }

    @Test
    public void prepare() {
        final String testQuery = "select * from test_table";
        sessionProxy.prepare(testQuery);
        Mockito.verify(wrappedSession, times(1)).execute(any(DefaultPrepareRequest.class), eq(PrepareRequest.SYNC));
    }

    @Test
    public void testPrepare() {
        final SimpleStatement statement = SimpleStatement.builder("select * from test_table").build();
        sessionProxy.prepare(statement);
        Mockito.verify(wrappedSession, times(1)).execute(any(DefaultPrepareRequest.class), eq(PrepareRequest.SYNC));
    }

    @Test
    public void prepareAsync() {
        final String testQuery = "select * from test_table";
        sessionProxy.prepareAsync(testQuery);
        Mockito.verify(wrappedSession, times(1)).execute(any(DefaultPrepareRequest.class), eq(PrepareRequest.ASYNC));
    }

    @Test
    public void testPrepareAsync() {
        final SimpleStatement statement = SimpleStatement.builder("select * from test_table").build();
        sessionProxy.prepareAsync(statement);
        Mockito.verify(wrappedSession, times(1)).execute(any(DefaultPrepareRequest.class), eq(PrepareRequest.ASYNC));
    }

    @Test
    public void closeAsync() {
        sessionProxy.closeAsync();
        Mockito.verify(wrappedSession, times(1)).closeAsync();
    }

    @Test
    public void forceCloseAsync() {
        sessionProxy.forceCloseAsync();
        Mockito.verify(wrappedSession, times(1)).forceCloseAsync();
    }

    @Test
    public void close() {
        sessionProxy.close();
        Mockito.verify(wrappedSession, times(1)).closeAsync();
    }

    @Test
    public void isClosed() {
        sessionProxy.isClosed();
        Mockito.verify(wrappedSession, times(1)).closeFuture();
    }

    @Test
    public void getName() {
        sessionProxy.getName();
        Mockito.verify(wrappedSession, times(1)).getName();
    }

    @Test
    public void getMetadata() {
        sessionProxy.getMetadata();
        Mockito.verify(wrappedSession, times(1)).getMetadata();
    }

    @Test
    public void isSchemaMetadataEnabled() {
        sessionProxy.isSchemaMetadataEnabled();
        Mockito.verify(wrappedSession, times(1)).isSchemaMetadataEnabled();
    }

    @Test
    public void setSchemaMetadataEnabled() {
        sessionProxy.setSchemaMetadataEnabled(false);
        Mockito.verify(wrappedSession, times(1)).setSchemaMetadataEnabled(false);
    }

    @Test
    public void refreshSchemaAsync() {
        sessionProxy.refreshSchemaAsync();
        Mockito.verify(wrappedSession, times(1)).refreshSchemaAsync();
    }

    @Test
    public void checkSchemaAgreementAsync() {
        sessionProxy.checkSchemaAgreementAsync();
        Mockito.verify(wrappedSession, times(1)).checkSchemaAgreementAsync();
    }

    @Test
    public void getContext() {
        sessionProxy.getContext();
        Mockito.verify(wrappedSession, times(1)).getContext();
    }

    @Test
    public void getMetrics() {
        sessionProxy.getMetrics();
        Mockito.verify(wrappedSession, times(1)).getMetrics();
    }

    @Test
    public void checkDatabaseConfig() {
        sessionProxy.getMetadata();
        verify(databasePool, times(1)).getOrCreateDatabase(eq(CassandraDBType.INSTANCE), eq(classifierBuilder.build()),
                eq(databaseConfig));
    }

    @Test
    public void checkWithPasswordCheck() {
        when(wrappedSession.getName()).thenThrow(AllNodesFailedException.class).thenReturn("test-name");
        sessionProxy.getName();
        verify(databasePool, times(2)).getOrCreateDatabase(eq(CassandraDBType.INSTANCE), eq(classifierBuilder.build()),
                eq(databaseConfig));
    }

    @Test
    public void checkReconnectDoesntHelp() {
        when(wrappedSession.getMetadata())
                .thenThrow(AllNodesFailedException.class);
        Assertions.assertThrows(AllNodesFailedException.class,
                () -> sessionProxy.getMetadata());
    }
}