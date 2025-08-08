package com.netcracker.cloud.dbaas.client.opensearch.restclient;

import com.netcracker.cloud.dbaas.client.entity.database.type.DatabaseType;
import com.netcracker.cloud.dbaas.client.management.DatabaseConfig;
import com.netcracker.cloud.dbaas.client.management.DatabasePool;
import com.netcracker.cloud.dbaas.client.management.DbaasDbClassifier;
import com.netcracker.cloud.dbaas.client.management.classifier.DbaaSClassifierBuilder;
import com.netcracker.cloud.dbaas.client.opensearch.DbaasOpensearchClientImpl;
import com.netcracker.cloud.dbaas.client.opensearch.config.OpensearchConfig;
import com.netcracker.cloud.dbaas.client.opensearch.entity.OpensearchDBType;
import com.netcracker.cloud.dbaas.client.opensearch.entity.OpensearchIndex;
import com.netcracker.cloud.dbaas.client.opensearch.entity.OpensearchIndexConnection;
import com.netcracker.cloud.dbaas.client.opensearch.entity.OpensearchProperties;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.transport.endpoints.BooleanResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class DbaasOpensearchClientImplTest {
    private static final String TEST_INDEX = "test_index";
    private static final String TEST_PREFIX = "test_prefix";
    private static final String TEST_FULL_INDEX_NAME = "test_prefix_test_index";

    private static DbaasOpensearchClientImpl dbaasOpensearchClient;
    private static OpenSearchClient openSearchClient;

    @BeforeAll
    public static void setUp() {
        openSearchClient = mock(OpenSearchClient.class);

        OpensearchIndexConnection opensearchIndexConnection = mock(OpensearchIndexConnection.class);
        when(opensearchIndexConnection.getResourcePrefix()).thenReturn(TEST_PREFIX);

        when(opensearchIndexConnection.getOpenSearchClient()).thenReturn(openSearchClient);

        dbaasOpensearchClient = Mockito.mock(DbaasOpensearchClientImpl.class);
        when(dbaasOpensearchClient.getOrCreateIndex()).thenReturn(opensearchIndexConnection);
        when(dbaasOpensearchClient.normalize(any())).thenCallRealMethod();
        when(dbaasOpensearchClient.normalize(any(OpensearchIndexConnection.class), any())).thenCallRealMethod();
        when(dbaasOpensearchClient.getDelimiter()).thenReturn("_");
    }

    private OpenSearchClient testGetOrCreateIndexSetup(DatabasePool databasePool, DbaaSClassifierBuilder dbaaSClassifierBuilder) {
        DbaasDbClassifier dbaasDbClassifier = mock(DbaasDbClassifier.class);
        when(dbaaSClassifierBuilder.build()).thenReturn(dbaasDbClassifier);

        OpensearchIndex opensearchIndex = mock(OpensearchIndex.class);
        when(databasePool.getOrCreateDatabase(any(), any(), any())).thenReturn(opensearchIndex);

        OpensearchIndexConnection opensearchIndexConnection = mock(OpensearchIndexConnection.class);
        when(opensearchIndex.getConnectionProperties()).thenReturn(opensearchIndexConnection);

        OpenSearchClient openSearchClientNew = mock(OpenSearchClient.class);
        when(opensearchIndexConnection.getOpenSearchClient()).thenReturn(openSearchClientNew);

        return openSearchClientNew;
    }

    @Test
    void testGetOpenSearchClient() {
        when(dbaasOpensearchClient.getClient()).thenCallRealMethod();
        assertEquals(openSearchClient, dbaasOpensearchClient.getClient());
    }

    @Test
    void testGetOrCreateIndexPasswordNotChanged() {
        DatabaseConfig.Builder databaseConfigBuilder = DatabaseConfig.builder();
        DatabasePool databasePool = mock(DatabasePool.class);
        DbaaSClassifierBuilder dbaaSClassifierBuilder = mock(DbaaSClassifierBuilder.class);
        OpensearchConfig opensearchConfig = mock(OpensearchConfig.class);
        testGetOrCreateIndexSetup(databasePool, dbaaSClassifierBuilder);

        DbaasOpensearchClientImpl dbaasOpensearchClientNew = new DbaasOpensearchClientImpl(databasePool, dbaaSClassifierBuilder, databaseConfigBuilder, opensearchConfig);
        dbaasOpensearchClientNew.getOrCreateIndex();
    }

    @Test
    void testGetOrCreateIndexWithGetDatabasePrefix() {
        DatabaseConfig.Builder databaseConfigBuilder = DatabaseConfig.builder();
        DatabasePool databasePool = mock(DatabasePool.class);
        DbaaSClassifierBuilder dbaaSClassifierBuilder = mock(DbaaSClassifierBuilder.class);
        OpensearchConfig opensearchConfig = mock(OpensearchConfig.class);
        OpensearchProperties opensearchProperties = new OpensearchProperties();
        OpensearchProperties.OpensearchDbScopeProperties servicePrefix = new OpensearchProperties.OpensearchDbScopeProperties();
        servicePrefix.setPrefix("test-prefix");
        servicePrefix.setDelimiter("--");
        opensearchProperties.setService(servicePrefix);
        when(opensearchConfig.getOpensearchProperties()).thenReturn(opensearchProperties);
        testGetOrCreateIndexSetup(databasePool, dbaaSClassifierBuilder);

        DbaasOpensearchClientImpl dbaasOpensearchClientNew = new DbaasOpensearchClientImpl(databasePool, dbaaSClassifierBuilder, databaseConfigBuilder, opensearchConfig);
        dbaasOpensearchClientNew.getOrCreateIndex();
    }

    @Test
    void testGetOrCreateIndexPasswordChanged() throws IOException {
        DatabasePool databasePool = mock(DatabasePool.class);
        DbaaSClassifierBuilder dbaaSClassifierBuilder = mock(DbaaSClassifierBuilder.class);

        OpenSearchClient restHighLevelClient = testGetOrCreateIndexSetup(databasePool, dbaaSClassifierBuilder);

        OpenSearchException unauthorizedException = mock(OpenSearchException.class);
        when(unauthorizedException.status()).thenReturn(HttpStatus.SC_UNAUTHORIZED);

        AtomicBoolean unauthorizedExceptionWasThrown = new AtomicBoolean();
        when(restHighLevelClient.exists(any(Function.class))).then(invocationOnMock -> {
            if (!unauthorizedExceptionWasThrown.get()) {
                unauthorizedExceptionWasThrown.set(true);
                throw unauthorizedException;
            } else {
                return new BooleanResponse(false);
            }
        });

        OpensearchConfig opensearchConfig = mock(OpensearchConfig.class);
        DatabaseConfig.Builder databaseConfigBuilder = DatabaseConfig.builder();

        DbaasOpensearchClientImpl dbaasOpensearchClientNew = new DbaasOpensearchClientImpl(databasePool, dbaaSClassifierBuilder, databaseConfigBuilder, opensearchConfig);
        OpensearchIndexConnection connection = dbaasOpensearchClientNew.getOrCreateIndex();

        assertNotNull(connection);
        Mockito.verify(databasePool, Mockito.times(2))
                .getOrCreateDatabase(eq(OpensearchDBType.INSTANCE), any(DbaasDbClassifier.class), any(DatabaseConfig.class));
        Mockito.verify(databasePool, Mockito.times(1)).removeCachedDatabase(any(DatabaseType.class), any(DbaasDbClassifier.class));
    }

    @Test
    void testGetOrCreateIndexTwiceUnauthorized() throws IOException {
        DatabasePool databasePool = mock(DatabasePool.class);
        DbaaSClassifierBuilder dbaaSClassifierBuilder = mock(DbaaSClassifierBuilder.class);
        OpensearchConfig opensearchConfig = mock(OpensearchConfig.class);
        DatabaseConfig.Builder databaseConfigBuilder = DatabaseConfig.builder();

        OpenSearchClient openSearchClientNew = testGetOrCreateIndexSetup(databasePool, dbaaSClassifierBuilder);

        OpenSearchException unauthorizedException = mock(OpenSearchException.class);
        when(unauthorizedException.status()).thenReturn(HttpStatus.SC_UNAUTHORIZED);
        when(openSearchClientNew.exists(any(Function.class))).thenThrow(unauthorizedException);

        DbaasOpensearchClientImpl dbaasOpensearchClientNew = new DbaasOpensearchClientImpl(databasePool, dbaaSClassifierBuilder, databaseConfigBuilder, opensearchConfig);
        assertThrows(IllegalStateException.class, dbaasOpensearchClientNew::getOrCreateIndex,
                "Authorization to Opensearch has been failed. Check credentials");

        Mockito.verify(databasePool, Mockito.times(2))
                .getOrCreateDatabase(eq(OpensearchDBType.INSTANCE), any(DbaasDbClassifier.class), any(DatabaseConfig.class));
        Mockito.verify(databasePool, Mockito.times(1)).removeCachedDatabase(any(DatabaseType.class), any(DbaasDbClassifier.class));
    }

    @Test
    void testGetOrCreateIndexOpenSearchRespondsUnexpectedException() throws IOException {
        DatabasePool databasePool = mock(DatabasePool.class);
        DbaaSClassifierBuilder dbaaSClassifierBuilder = mock(DbaaSClassifierBuilder.class);

        OpenSearchClient openSearchClientNew = testGetOrCreateIndexSetup(databasePool, dbaaSClassifierBuilder);

        OpensearchConfig opensearchConfig = mock(OpensearchConfig.class);
        DatabaseConfig.Builder databaseConfigBuilder = DatabaseConfig.builder();

        OpenSearchException unexpectedException = mock(OpenSearchException.class);
        when(openSearchClientNew.exists(any(Function.class))).thenThrow(unexpectedException);

        DbaasOpensearchClientImpl dbaasOpensearchClientNew = new DbaasOpensearchClientImpl(databasePool, dbaaSClassifierBuilder, databaseConfigBuilder, opensearchConfig);
        assertThrows(OpenSearchException.class, dbaasOpensearchClientNew::getOrCreateIndex);

        Mockito.verify(databasePool, Mockito.times(1))
                .getOrCreateDatabase(eq(OpensearchDBType.INSTANCE), any(DbaasDbClassifier.class), any(DatabaseConfig.class));
    }

    @Test
    void normalizeTest() {
        assertEquals(TEST_FULL_INDEX_NAME, dbaasOpensearchClient.normalize(TEST_INDEX));
    }

    @Test
    void normalizeWithConnectionTest() {
        OpensearchIndexConnection opensearchIndexConnection = mock(OpensearchIndexConnection.class);
        when(opensearchIndexConnection.getResourcePrefix()).thenReturn(TEST_PREFIX);
        when(dbaasOpensearchClient.getDelimiter()).thenReturn("--");
        when(opensearchIndexConnection.getOpenSearchClient()).thenReturn(openSearchClient);

        when(dbaasOpensearchClient.getOrCreateIndex(any())).thenReturn(opensearchIndexConnection);
        OpensearchIndexConnection customIndex = dbaasOpensearchClient.getOrCreateIndex(DatabaseConfig.builder().build());
        assertEquals("test_prefix--" + TEST_INDEX, dbaasOpensearchClient.normalize(customIndex, TEST_INDEX));
        when(dbaasOpensearchClient.getDelimiter()).thenReturn("_");
    }
}
