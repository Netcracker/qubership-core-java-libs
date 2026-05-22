package com.netcracker.cloud.quarkus.dbaas.cassandraclient;

import com.datastax.oss.driver.api.core.AllNodesFailedException;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.session.Request;
import com.datastax.oss.driver.api.core.type.reflect.GenericType;
import com.netcracker.cloud.dbaas.client.cassandra.entity.connection.CassandraDBConnection;
import com.netcracker.cloud.dbaas.client.cassandra.entity.database.CassandraDatabase;
import com.netcracker.cloud.dbaas.client.cassandra.service.CassandraLogicalDbProvider;
import com.netcracker.cloud.dbaas.client.entity.DbaasApiProperties;
import com.netcracker.cloud.dbaas.client.management.DbaasDbClassifier;
import com.netcracker.cloud.dbaas.client.management.classifier.DbaaSClassifierBuilder;
import com.netcracker.cloud.dbaas.common.config.DbaasApiPropertiesConfig;
import com.netcracker.cloud.dbaas.common.postprocessor.PostConnectProcessorManager;
import com.netcracker.cloud.quarkus.dbaas.cassandraclient.config.CassandraClientConfiguration;
import com.netcracker.cloud.quarkus.dbaas.cassandraclient.config.properties.CassandraProperties;
import com.netcracker.cloud.quarkus.dbaas.cassandraclient.config.properties.DbaaSCassandraDbCreationConfig;
import com.netcracker.cloud.quarkus.dbaas.cassandraclient.service.CassandraClientCreation;
import com.netcracker.cloud.quarkus.dbaas.cassandraclient.service.CqlSessionCreator;
import com.netcracker.cloud.quarkus.dbaas.cassandraclient.service.impl.CassandraClientCreationImpl;
import jakarta.enterprise.inject.Instance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DbaaSCassandraClientTest {

    CassandraClientCreation cassandraClientCreation;
    DbaaSClassifierBuilder dbaaSClassifierBuilder;
    CqlSession cqlSession;

    @BeforeEach
    void prepareMocks() {
        dbaaSClassifierBuilder = mock(DbaaSClassifierBuilder.class);
        when(dbaaSClassifierBuilder.build()).thenReturn(new DbaasDbClassifier(Map.of("scope", "service")));
        cqlSession = mock(CqlSession.class);
        CassandraDBConnection cassandraDBConnection = new CassandraDBConnection();
        cassandraDBConnection.setSession(cqlSession);
        CassandraDatabase cassandraDatabase = new CassandraDatabase();
        cassandraDatabase.setConnectionProperties(cassandraDBConnection);
        cassandraClientCreation = mock(CassandraClientCreation.class);
        when(cassandraClientCreation.getOrCreateCassandraDatabase(any())).thenReturn(cassandraDatabase);
    }

    @Test
    void testGetName() {
        DbaaSCassandraClient dbaaSCassandraClient = new DbaaSCassandraClient(dbaaSClassifierBuilder, cassandraClientCreation);
        dbaaSCassandraClient.getName();
        Mockito.verify(cqlSession, only()).getName();
    }

    @Test
    void testGetMetadata() {
        DbaaSCassandraClient dbaaSCassandraClient = new DbaaSCassandraClient(dbaaSClassifierBuilder, cassandraClientCreation);
        dbaaSCassandraClient.getMetadata();
        Mockito.verify(cqlSession, only()).getMetadata();
    }

    @Test
    void testIsSchemaMetadataEnabled() {
        DbaaSCassandraClient dbaaSCassandraClient = new DbaaSCassandraClient(dbaaSClassifierBuilder, cassandraClientCreation);
        dbaaSCassandraClient.isSchemaMetadataEnabled();
        Mockito.verify(cqlSession, only()).isSchemaMetadataEnabled();
    }

    @Test
    void testSetSchemaMetadataEnabled() {
        DbaaSCassandraClient dbaaSCassandraClient = new DbaaSCassandraClient(dbaaSClassifierBuilder, cassandraClientCreation);
        dbaaSCassandraClient.setSchemaMetadataEnabled(true);
        Mockito.verify(cqlSession, only()).setSchemaMetadataEnabled(any());
    }

    @Test
    void testRefreshSchemaAsync() {
        DbaaSCassandraClient dbaaSCassandraClient = new DbaaSCassandraClient(dbaaSClassifierBuilder, cassandraClientCreation);
        dbaaSCassandraClient.refreshSchemaAsync();
        Mockito.verify(cqlSession, only()).refreshSchemaAsync();
    }

    @Test
    void testCheckSchemaAgreementAsync() {
        DbaaSCassandraClient dbaaSCassandraClient = new DbaaSCassandraClient(dbaaSClassifierBuilder, cassandraClientCreation);
        dbaaSCassandraClient.checkSchemaAgreementAsync();
        Mockito.verify(cqlSession, only()).checkSchemaAgreementAsync();

    }

    @Test
    void testGetContext() {
        DbaaSCassandraClient dbaaSCassandraClient = new DbaaSCassandraClient(dbaaSClassifierBuilder, cassandraClientCreation);
        dbaaSCassandraClient.getContext();
        Mockito.verify(cqlSession, only()).getContext();
    }

    @Test
    void testGetKeyspace() {
        DbaaSCassandraClient dbaaSCassandraClient = new DbaaSCassandraClient(dbaaSClassifierBuilder, cassandraClientCreation);
        dbaaSCassandraClient.getKeyspace();
        Mockito.verify(cqlSession, only()).getKeyspace();
    }

    @Test
    void testGetMetrics() {
        DbaaSCassandraClient dbaaSCassandraClient = new DbaaSCassandraClient(dbaaSClassifierBuilder, cassandraClientCreation);
        dbaaSCassandraClient.getMetrics();
        Mockito.verify(cqlSession, only()).getMetrics();
    }

    @Test
    <R extends Request, T> void testExecute() {
        DbaaSCassandraClient dbaaSCassandraClient = new DbaaSCassandraClient(dbaaSClassifierBuilder, cassandraClientCreation);
        dbaaSCassandraClient.execute((R) null, (GenericType<T>) null);
        Mockito.verify(cqlSession, only()).execute((R) any(), (GenericType<T>) any());
    }

    @Test
    void testCloseFuture() {
        DbaaSCassandraClient dbaaSCassandraClient = new DbaaSCassandraClient(dbaaSClassifierBuilder, cassandraClientCreation);
        dbaaSCassandraClient.closeFuture();
        Mockito.verify(cqlSession, only()).closeFuture();
    }

    @Test
    void testCloseAsync() {
        DbaaSCassandraClient dbaaSCassandraClient = new DbaaSCassandraClient(dbaaSClassifierBuilder, cassandraClientCreation);
        dbaaSCassandraClient.closeAsync();
        Mockito.verify(cqlSession, only()).closeAsync();
    }

    @Test
    void testForceCloseAsync() {
        DbaaSCassandraClient dbaaSCassandraClient = new DbaaSCassandraClient(dbaaSClassifierBuilder, cassandraClientCreation);
        dbaaSCassandraClient.forceCloseAsync();
        Mockito.verify(cqlSession, only()).forceCloseAsync();
    }

    @Test
    <R extends Request> void testExecuteEvictAndRetryOnAllNodesFailedException() {
        CqlSession staleSession = mock(CqlSession.class);
        CqlSession freshSession = mock(CqlSession.class);

        when(staleSession.execute((R) any(), any())).thenThrow(AllNodesFailedException.class);

        CqlSessionCreator cqlSessionCreator = mock(CqlSessionCreator.class);
        when(cqlSessionCreator.createSession(any(CassandraDatabase.class)))
                .thenReturn(staleSession)
                .thenReturn(freshSession);

        CassandraClientCreationImpl creationImpl = createCreationImpl(cqlSessionCreator);
        DbaaSCassandraClient client = new DbaaSCassandraClient(dbaaSClassifierBuilder, creationImpl);

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> client.execute((R) null, null),
                "Exception should lead to the old connection eviction and creation of a new one");

        verify(freshSession, times(1)).execute((R) any(), any());
    }

    @Test
    <R extends Request> void testExecuteAllNodesFailedExceptionPropagatesWhenRetryAlsoFails() {
        // Both the original and retry session fail: exception must propagate to the caller.
        CqlSession alwaysFailSession = mock(CqlSession.class);
        when(alwaysFailSession.execute((R) any(), any()))
                .thenThrow(AllNodesFailedException.class);

        CqlSessionCreator cqlSessionCreator = mock(CqlSessionCreator.class);
        when(cqlSessionCreator.createSession(any(CassandraDatabase.class)))
                .thenReturn(alwaysFailSession);

        CassandraClientCreationImpl realCreation = createCreationImpl(cqlSessionCreator);
        DbaaSCassandraClient client = new DbaaSCassandraClient(dbaaSClassifierBuilder, realCreation);

        org.junit.jupiter.api.Assertions.assertThrows(AllNodesFailedException.class,
                () -> client.execute((R) null, null));
    }

    @Test
    void testCorrectBaseClassifierCreation() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        CassandraClientConfiguration cassandraClientConfiguration = new CassandraClientConfiguration();
        Method method = cassandraClientConfiguration.getClass().getDeclaredMethod("getInitialClassifierMap");
        method.setAccessible(true);

        Field namespace = cassandraClientConfiguration.getClass().getDeclaredField("namespace");
        namespace.setAccessible(true);
        namespace.set(cassandraClientConfiguration, "test-namespace");

        Field microserviceName = cassandraClientConfiguration.getClass().getDeclaredField("microserviceName");
        microserviceName.setAccessible(true);
        microserviceName.set(cassandraClientConfiguration, "test-microserviceName");

        CassandraProperties properties = mock(CassandraProperties.class);
        DbaaSCassandraDbCreationConfig creationConfig = mock(DbaaSCassandraDbCreationConfig.class);
        when(creationConfig.dbClassifier()).thenReturn("test");
        when(properties.cassandraDbCreationConfig()).thenReturn(creationConfig);
        Field cassandraProperties = cassandraClientConfiguration.getClass().getDeclaredField("cassandraProperties");
        cassandraProperties.setAccessible(true);
        cassandraProperties.set(cassandraClientConfiguration, properties);

        Map<String, Object> classifeir = new HashMap<>();
        classifeir.put("dbClassifier", "test");
        classifeir.put("microserviceName", "test-microserviceName");
        classifeir.put("namespace", "test-namespace");
        assertEquals(classifeir, method.invoke(cassandraClientConfiguration));
    }

    private CassandraClientCreationImpl createCreationImpl(CqlSessionCreator cqlSessionCreator) {
        CassandraProperties cassandraProperties = mock(CassandraProperties.class);
        DbaaSCassandraDbCreationConfig creationConfig = mock(DbaaSCassandraDbCreationConfig.class);
        when(creationConfig.getCassandraDbConfiguration(any())).thenReturn(null);
        DbaasApiPropertiesConfig apiPropertiesConfig = mock(DbaasApiPropertiesConfig.class);
        when(apiPropertiesConfig.getDbaaseApiProperties()).thenReturn(new DbaasApiProperties());
        when(creationConfig.dbaasApiPropertiesConfig()).thenReturn(apiPropertiesConfig);
        when(cassandraProperties.cassandraDbCreationConfig()).thenReturn(creationConfig);

        CassandraLogicalDbProvider provider = mock(CassandraLogicalDbProvider.class);
        when(provider.order()).thenReturn(0);
        when(provider.provide(any(), any(), any())).thenAnswer(inv -> {
            CassandraDBConnection conn = new CassandraDBConnection();
            CassandraDatabase db = new CassandraDatabase();
            db.setConnectionProperties(conn);
            return db;
        });
        Instance<CassandraLogicalDbProvider> dbProviders = mock(Instance.class);
        when(dbProviders.stream()).thenAnswer(inv -> Stream.of(provider));

        PostConnectProcessorManager<CassandraDatabase> postConnectProcessorManager = mock(PostConnectProcessorManager.class);

        return new CassandraClientCreationImpl("test-namespace", cassandraProperties,
                dbProviders, cqlSessionCreator, postConnectProcessorManager);
    }
}
