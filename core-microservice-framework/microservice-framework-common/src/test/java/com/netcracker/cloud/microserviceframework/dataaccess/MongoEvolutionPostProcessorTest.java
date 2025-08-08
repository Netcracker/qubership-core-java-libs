package com.netcracker.cloud.microserviceframework.dataaccess;

import com.mongodb.MongoCredential;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.netcracker.cloud.dbaas.client.entity.connection.MongoDBConnection;
import com.netcracker.cloud.dbaas.client.entity.database.MongoDatabase;
import com.netcracker.cloud.microserviceframework.config.MongoPackagesConfigHolder;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MongoEvolutionPostProcessorTest {

    private MongoEvolutionPostProcessor processor;

    @Mock
    private Environment environment;

    @Mock
    private MongoPackagesConfigHolder configHolder;

    @Mock
    private MongoDatabase mongoDatabase;

    @Mock
    private MongoDBConnection mongoDBConnection;

    @BeforeEach
    void setUp() {
        processor = new MongoEvolutionPostProcessor(environment);
        ReflectionTestUtils.setField(processor, "configHolderMongo", configHolder);
        ReflectionTestUtils.setField(processor, "authEnabled", true);
    }

    @Test
    void testGetSupportedDatabaseType() {
        assertEquals(MongoDatabase.class, processor.getSupportedDatabaseType());
    }

    @Test
    void testGetMongoCredential() throws Exception {
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
    void testInitWithCustomPackages() throws Exception {
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
    void testInitWithDefaultPackages() throws Exception {
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
    void testProcessWithEmptyPackages() {
        // Given
        setupEmptyPackagesConfig();

        // When
        processor.process(mongoDatabase);

        // Then
        verify(mongoDatabase, times(2)).getClassifier();
    }

    @Test
    void testProcessWithValidPackages() {
        // Given
        setupValidPackagesConfig();

        // When
        processor.process(mongoDatabase);

        // Then
        verify(mongoDatabase, times(2)).getClassifier();
    }

    @Test
    void testProcessWithNullTenantId() {
        // Given
        setupNullTenantIdConfig();

        // When
        processor.process(mongoDatabase);

        // Then
        verify(mongoDatabase, times(2)).getClassifier();
    }

    @Test
    void testProcessWithNullDbClassifier() {
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
