package org.qubership.cloud.quarkus.dbaas.opensearch.client;

import org.qubership.cloud.dbaas.client.management.DatabaseConfig;
import org.qubership.cloud.dbaas.client.management.DbaasDbClassifier;
import org.qubership.cloud.dbaas.client.management.classifier.DbaaSClassifierBuilder;
import org.qubership.cloud.dbaas.client.opensearch.entity.OpensearchIndexConnection;
import org.qubership.cloud.quarkus.dbaas.opensearch.client.service.OpensearchDbaaSApiClient;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.transport.endpoints.BooleanResponse;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DbaaSOpensearchClientTest {
    OpensearchDbaaSApiClient opensearchDbaaSApiClient;

    DbaaSClassifierBuilder dbaaSClassifierBuilder;

    DbaasDbClassifier mockClassifier;

    OpenSearchClient client;

    OpensearchIndexConnection opensearchConnection;

    private static final String TEST_PREFIX = "test";
    private static final String TEST_USERNAME = "username";

    @BeforeEach
    void prepareMocks() {
        dbaaSClassifierBuilder = mock(DbaaSClassifierBuilder.class);
        mockClassifier = mock(DbaasDbClassifier.class);
        client = mock(OpenSearchClient.class);
        opensearchConnection = new OpensearchIndexConnection();
        opensearchConnection.setOpenSearchClient(client);
        opensearchConnection.setResourcePrefix(TEST_PREFIX);
        opensearchConnection.setUsername(TEST_USERNAME);
        opensearchDbaaSApiClient = mock(OpensearchDbaaSApiClient.class);
        when(dbaaSClassifierBuilder.build()).thenReturn(mockClassifier);
        when(opensearchDbaaSApiClient.getOpensearchIndex(any())).thenReturn(opensearchConnection);
        when(opensearchDbaaSApiClient.getOrCreateOpensearchIndex(any())).thenReturn(opensearchConnection);
    }

    @Test
    void testGetPrefix() {
        DbaasOpensearchClientImpl dbaaSOpensearchClient = new DbaasOpensearchClientImpl(dbaaSClassifierBuilder, opensearchDbaaSApiClient, "_");
        String actualPrefix = dbaaSOpensearchClient.getPrefix();
        assertEquals(TEST_PREFIX, actualPrefix);
    }

    @Test
    void testGetDbaasIndex() {
        DbaasOpensearchClientImpl dbaaSOpensearchClient = new DbaasOpensearchClientImpl(dbaaSClassifierBuilder, opensearchDbaaSApiClient, "_");
        assertEquals(TEST_PREFIX, dbaaSOpensearchClient.getPrefix());
    }

    @Test
    void testGetOrCreateIndex() {
        when(opensearchDbaaSApiClient.getOrCreateOpensearchIndex(mockClassifier)).thenReturn(opensearchConnection);
        DbaasOpensearchClientImpl dbaaSOpensearchClient = new DbaasOpensearchClientImpl(dbaaSClassifierBuilder, opensearchDbaaSApiClient, "_");
        OpensearchIndexConnection dbaaSIndex = dbaaSOpensearchClient.getOrCreateIndex();
        assertEquals(TEST_PREFIX, dbaaSIndex.getResourcePrefix());
        assertEquals(TEST_USERNAME, dbaaSIndex.getUsername());
    }

    @Test
    void testGetOrCreateIndexWithDbParameters() {
        DatabaseConfig params = DatabaseConfig.builder().build();
        when(opensearchDbaaSApiClient.getOrCreateOpensearchIndex(params, mockClassifier)).thenReturn(opensearchConnection);

        DbaasOpensearchClientImpl dbaaSOpensearchClient = new DbaasOpensearchClientImpl(dbaaSClassifierBuilder, opensearchDbaaSApiClient, "_");
        OpensearchIndexConnection dbaaSIndex = dbaaSOpensearchClient.getOrCreateIndex(params);
        assertEquals(TEST_PREFIX, dbaaSIndex.getResourcePrefix());
        assertEquals(TEST_USERNAME, dbaaSIndex.getUsername());
    }

    @Test
    void testCorrectBaseClassifierCreation() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        DbaasOpensearchConfiguration dbaasOpensearchConfiguration = new DbaasOpensearchConfiguration();
        Method method = dbaasOpensearchConfiguration.getClass().getDeclaredMethod("getInitialClassifierMap");
        method.setAccessible(true);

        Field namespace = dbaasOpensearchConfiguration.getClass().getDeclaredField("namespace");
        namespace.setAccessible(true);
        namespace.set(dbaasOpensearchConfiguration, "test-namespace");

        Field microserviceName = dbaasOpensearchConfiguration.getClass().getDeclaredField("microserviceName");
        microserviceName.setAccessible(true);
        microserviceName.set(dbaasOpensearchConfiguration, "test-microserviceName");

        Map<String, Object> classifeir = new HashMap<>();
        classifeir.put("microserviceName", "test-microserviceName");
        classifeir.put("namespace", "test-namespace");
        assertEquals(classifeir, method.invoke(dbaasOpensearchConfiguration));
    }

    @Test
    void testGetOrCreateIndexPasswordChanged() throws IOException {
        OpensearchIndexConnection opensearchConnection2 = spy(opensearchConnection);
        OpenSearchException unauthorizedException = mock(OpenSearchException.class);
        OpenSearchClient openSearchClient = mock(OpenSearchClient.class);

        when(opensearchDbaaSApiClient.getOrCreateOpensearchIndex(mockClassifier)).thenReturn(opensearchConnection2);
        DbaasOpensearchClientImpl dbaaSOpensearchClient = new DbaasOpensearchClientImpl(dbaaSClassifierBuilder, opensearchDbaaSApiClient, "_");

        when(opensearchConnection2.getOpenSearchClient()).thenReturn(openSearchClient);
        when(unauthorizedException.status()).thenReturn(HttpStatus.SC_UNAUTHORIZED);

        AtomicBoolean unauthorizedExceptionWasThrown = new AtomicBoolean();
        when(openSearchClient.exists(any(Function.class))).then(invocationOnMock -> {
            if (!unauthorizedExceptionWasThrown.get()) {
                unauthorizedExceptionWasThrown.set(true);
                throw unauthorizedException;
            } else {
                return new BooleanResponse(false);
            }
        });

        OpensearchIndexConnection connection = dbaaSOpensearchClient.getOrCreateIndex();
        assertEquals(TEST_PREFIX, connection.getResourcePrefix());
        assertEquals(TEST_USERNAME, connection.getUsername());

        assertNotNull(connection);
        Mockito.verify(opensearchDbaaSApiClient, Mockito.times(2))
                .getOrCreateOpensearchIndex(any(DbaasDbClassifier.class));
        Mockito.verify(opensearchDbaaSApiClient, Mockito.times(1)).removeCachedDatabase(any(DbaasDbClassifier.class));
    }

    @Test
    void testGetOrCreateIndexTwiceUnauthorized() throws IOException {
        OpensearchIndexConnection opensearchConnection2 = spy(opensearchConnection);
        OpenSearchException unauthorizedException = mock(OpenSearchException.class);
        OpenSearchClient openSearchClient = mock(OpenSearchClient.class);

        when(opensearchDbaaSApiClient.getOrCreateOpensearchIndex(mockClassifier)).thenReturn(opensearchConnection2);
        DbaasOpensearchClientImpl dbaaSOpensearchClient = new DbaasOpensearchClientImpl(dbaaSClassifierBuilder, opensearchDbaaSApiClient, "_");

        when(opensearchConnection2.getOpenSearchClient()).thenReturn(openSearchClient);
        when(unauthorizedException.status()).thenReturn(HttpStatus.SC_UNAUTHORIZED);
        when(openSearchClient.exists(any(Function.class))).thenThrow(unauthorizedException);

        assertThrows(IllegalStateException.class, dbaaSOpensearchClient::getOrCreateIndex,
                "Authorization to Opensearch has been failed. Check credentials");

        Mockito.verify(opensearchDbaaSApiClient, Mockito.times(2))
                .getOrCreateOpensearchIndex(any(DbaasDbClassifier.class));
        Mockito.verify(opensearchDbaaSApiClient, Mockito.times(1)).removeCachedDatabase(any(DbaasDbClassifier.class));
    }

    @Test
    void testGetOrCreateIndexOpenSearchRespondsUnexpectedException() throws IOException {
        OpensearchIndexConnection opensearchConnection2 = spy(opensearchConnection);
        OpenSearchClient openSearchClient = mock(OpenSearchClient.class);

        when(opensearchDbaaSApiClient.getOrCreateOpensearchIndex(mockClassifier)).thenReturn(opensearchConnection2);
        DbaasOpensearchClientImpl dbaaSOpensearchClient = new DbaasOpensearchClientImpl(dbaaSClassifierBuilder, opensearchDbaaSApiClient, "_");

        when(opensearchConnection2.getOpenSearchClient()).thenReturn(openSearchClient);

        OpenSearchException unexpectedException = mock(OpenSearchException.class);
        when(openSearchClient.exists(any(Function.class))).thenThrow(unexpectedException);

        assertThrows(OpenSearchException.class, dbaaSOpensearchClient::getOrCreateIndex);

        Mockito.verify(opensearchDbaaSApiClient, Mockito.times(1))
                .getOrCreateOpensearchIndex(any(DbaasDbClassifier.class));
    }
}