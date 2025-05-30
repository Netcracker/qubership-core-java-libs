package org.qubership.cloud.microserviceframework.dataaccess;

import com.mongodb.MongoCredential;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.qubership.cloud.dbaas.client.entity.connection.MongoDBConnection;
import org.qubership.cloud.dbaas.client.entity.database.MongoDatabase;
import org.qubership.cloud.microserviceframework.config.MongoPackagesConfigHolder;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class MongoEvolutionPostProcessorTest {

    private MongoEvolutionPostProcessor processor;

    @Mock
    private Environment environment;

    @Mock
    private MongoPackagesConfigHolder configHolder;

    @Mock
    private MongoDatabase mongoDatabase;

    @Mock
    private MongoDBConnection mongoDBConnection;

    @Before
    public void setUp() {
        processor = new MongoEvolutionPostProcessor(environment);
        ReflectionTestUtils.setField(processor, "configHolderMongo", configHolder);
        ReflectionTestUtils.setField(processor, "authEnabled", true);
    }

    @Test
    public void testGetSupportedDatabaseType() {
        assertEquals(MongoDatabase.class, processor.getSupportedDatabaseType());
    }

    @Test
    public void testGetMongoCredential() throws Exception {
        // Given
        when(mongoDBConnection.getUsername()).thenReturn("testUser");
        when(mongoDBConnection.getAuthDbName()).thenReturn("testAuthDb");
        when(mongoDBConnection.getPassword()).thenReturn("testPassword");

        // When
        Method getMongoCredentialMethod = MongoEvolutionPostProcessor.class.getDeclaredMethod("getMongoCredential", MongoDBConnection.class);
        getMongoCredentialMethod.setAccessible(true);
        MongoCredential credential = (MongoCredential) getMongoCredentialMethod.invoke(processor, mongoDBConnection);

        // Then
        assertNotNull(credential);
        assertEquals("testUser", credential.getUserName());
        assertEquals("testAuthDb", credential.getSource());
    }

    @Test
    public void testInitWithCustomPackages() throws Exception {
        // Given
        setupCustomPackagesConfig();

        // When
        Method initMethod = MongoEvolutionPostProcessor.class.getDeclaredMethod("init");
        initMethod.setAccessible(true);
        initMethod.invoke(processor);

        // Then
        verifyCustomPackagesInitialization();
    }

    @Test
    public void testInitWithDefaultPackages() throws Exception {
        // Given
        setupDefaultPackagesConfig();

        // When
        Method initMethod = MongoEvolutionPostProcessor.class.getDeclaredMethod("init");
        initMethod.setAccessible(true);
        initMethod.invoke(processor);

        // Then
        verifyDefaultPackagesInitialization();
    }

    @Test
    public void testProcessWithEmptyPackages() {
        // Given
        setupEmptyPackagesConfig();

        // When
        processor.process(mongoDatabase);

        // Then
        verify(mongoDatabase, times(2)).getClassifier();
    }

    @Test
    public void testProcessWithValidPackages() {
        // Given
        setupValidPackagesConfig();

        // When
        processor.process(mongoDatabase);

        // Then
        verify(mongoDatabase, times(2)).getClassifier();
    }

    @Test
    public void testProcessWithNullTenantId() {
        // Given
        setupNullTenantIdConfig();

        // When
        processor.process(mongoDatabase);

        // Then
        verify(mongoDatabase, times(2)).getClassifier();
    }

    @Test
    public void testProcessWithNullDbClassifier() {
        // Given
        setupNullDbClassifierConfig();

        // When
        processor.process(mongoDatabase);

        // Then
        verify(mongoDatabase, times(2)).getClassifier();
    }

    private void setupCustomPackagesConfig() {
        when(configHolder.isUsingCustomPackages()).thenReturn(true);
        when(configHolder.getMicroserviceCustomPackage()).thenReturn("com.test.microservice");
        when(configHolder.getTenantAwareCustomPackage()).thenReturn("com.test.tenant");
        when(configHolder.getCommonPackage()).thenReturn("com.test.common");
    }

    private void setupDefaultPackagesConfig() {
        when(configHolder.isUsingCustomPackages()).thenReturn(false);
        when(configHolder.getDefaultPackage()).thenReturn("com.test.default");
        when(configHolder.getCommonPackage()).thenReturn("com.test.common");
    }

    private void setupEmptyPackagesConfig() {
        SortedMap<String, Object> emptyClassifier = new TreeMap<>();
        when(configHolder.isUsingCustomPackages()).thenReturn(true);
        when(mongoDatabase.getClassifier()).thenReturn(emptyClassifier);
        when(mongoDatabase.getConnectionProperties()).thenReturn(mongoDBConnection);
    }

    private void setupValidPackagesConfig() {
        SortedMap<String, Object> classifier = new TreeMap<>();
        classifier.put("tenantId", "testTenant");
        classifier.put("dbClassifier", "testClassifier");

        when(configHolder.isUsingCustomPackages()).thenReturn(true);
        when(mongoDatabase.getClassifier()).thenReturn(classifier);
        when(mongoDatabase.getConnectionProperties()).thenReturn(mongoDBConnection);
    }

    private void setupNullTenantIdConfig() {
        SortedMap<String, Object> classifier = new TreeMap<>();
        classifier.put("dbClassifier", "testClassifier");

        when(configHolder.isUsingCustomPackages()).thenReturn(true);
        when(mongoDatabase.getClassifier()).thenReturn(classifier);
        when(mongoDatabase.getConnectionProperties()).thenReturn(mongoDBConnection);
    }

    private void setupNullDbClassifierConfig() {
        SortedMap<String, Object> classifier = new TreeMap<>();
        classifier.put("tenantId", "testTenant");

        when(configHolder.isUsingCustomPackages()).thenReturn(true);
        when(mongoDatabase.getClassifier()).thenReturn(classifier);
        when(mongoDatabase.getConnectionProperties()).thenReturn(mongoDBConnection);
    }

    private void verifyCustomPackagesInitialization() {
        @SuppressWarnings("unchecked")
        List<String> microservicePackages = (List<String>) ReflectionTestUtils.getField(processor, "microservicePackages");
        @SuppressWarnings("unchecked")
        List<String> tenantAwarePackages = (List<String>) ReflectionTestUtils.getField(processor, "tenantAwarePackages");

        assertNotNull(microservicePackages);
        assertNotNull(tenantAwarePackages);
        assertEquals(2, microservicePackages.size());
        assertEquals(2, tenantAwarePackages.size());
    }

    private void verifyDefaultPackagesInitialization() {
        @SuppressWarnings("unchecked")
        List<String> defaultPackages = (List<String>) ReflectionTestUtils.getField(processor, "defaultPackages");

        assertNotNull(defaultPackages);
        assertEquals(2, defaultPackages.size());
    }
}
