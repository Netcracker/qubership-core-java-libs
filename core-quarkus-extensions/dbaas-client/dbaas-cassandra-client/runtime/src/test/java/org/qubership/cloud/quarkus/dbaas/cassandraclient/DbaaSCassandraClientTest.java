package org.qubership.cloud.quarkus.dbaas.cassandraclient;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.session.Request;
import com.datastax.oss.driver.api.core.type.reflect.GenericType;
import org.qubership.cloud.dbaas.client.cassandra.entity.connection.CassandraDBConnection;
import org.qubership.cloud.dbaas.client.cassandra.entity.database.CassandraDatabase;
import org.qubership.cloud.dbaas.client.management.classifier.DbaaSClassifierBuilder;
import org.qubership.cloud.quarkus.dbaas.cassandraclient.config.CassandraClientConfiguration;
import org.qubership.cloud.quarkus.dbaas.cassandraclient.config.properties.CassandraProperties;
import org.qubership.cloud.quarkus.dbaas.cassandraclient.config.properties.DbaaSCassandraDbCreationConfig;
import org.qubership.cloud.quarkus.dbaas.cassandraclient.service.CassandraClientCreation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.when;

class DbaaSCassandraClientTest {

    CassandraClientCreation cassandraClientCreation;
    DbaaSClassifierBuilder dbaaSClassifierBuilder;
    CqlSession cqlSession;

    @BeforeEach
    void prepareMocks() {
        dbaaSClassifierBuilder = mock(DbaaSClassifierBuilder.class);
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
        when(creationConfig.getDbClassifier()).thenReturn("test");
        when(properties.getCassandraDbCreationConfig()).thenReturn(creationConfig);
        Field cassandraProperties = cassandraClientConfiguration.getClass().getDeclaredField("cassandraProperties");
        cassandraProperties.setAccessible(true);
        cassandraProperties.set(cassandraClientConfiguration, properties);

        Map<String, Object> classifeir = new HashMap<>();
        classifeir.put("dbClassifier", "test");
        classifeir.put("microserviceName", "test-microserviceName");
        classifeir.put("namespace", "test-namespace");
        assertEquals(classifeir, method.invoke(cassandraClientConfiguration));
    }
}
