package org.qubership.cloud.dbaas.client.management;

import org.qubership.cloud.dbaas.client.DbaasClient;
import org.qubership.cloud.dbaas.client.DbaasConst;
import org.qubership.cloud.dbaas.client.entity.database.DatabaseSettings;
import org.qubership.cloud.dbaas.client.entity.test.*;
import org.qubership.cloud.dbaas.client.management.classifier.ServiceDbaaSClassifierBuilder;
import org.qubership.cloud.dbaas.client.service.LogicalDbProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import static org.qubership.cloud.dbaas.client.DbaasConst.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class DatabasePoolTest {
    private static final String TEST_MS_NAME = "test-ms";
    private static final String TEST_NAMESPACE = "test-namespace";

    private final DbaasClient dbaasClient = Mockito.mock(DbaasClient.class);
    private final PostConnectProcessor<TestDatabase> postConnectProcessor = Mockito.mock(PostConnectProcessor.class);

    private final DbaasDbClassifier classifier = new ServiceDbaaSClassifierBuilder(null).build();

    private TestDatabase testDatabase;
    private DatabaseConfig.Builder databaseConfigBuilder;

    @BeforeEach
    public void setUp() {
        Mockito.reset(dbaasClient, postConnectProcessor);
        this.databaseConfigBuilder = DatabaseConfig.builder();
        Mockito.when(postConnectProcessor.getSupportedDatabaseType()).thenReturn(TestDatabase.class);

        testDatabase = new TestDatabase();
        testDatabase.setNamespace(TEST_NAMESPACE);

        Mockito.when(dbaasClient.getOrCreateDatabase(
                eq(TestDBType.INSTANCE),
                eq(TEST_NAMESPACE),
                any(),
                any(DatabaseConfig.class))).thenReturn(testDatabase);
    }

    @Test
    public void testGetOrCreateDatabase() {
        DatabasePool databasePool = getDatabasePool();

        TestDatabase receivedDatabase = databasePool.getOrCreateDatabase(TestDBType.INSTANCE, classifier);
        assertEquals(testDatabase, receivedDatabase);
        Mockito.verify(postConnectProcessor, times(1)).process(testDatabase);
    }

    @Test
    void testEnrichClassifierCustomMicroserviceNameAndNamespace() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        DatabasePool databasePool = getDatabasePool();

        var customMicroserviceName= "customMicroserviceName";
        var customNamespace= "customNamespace";
        var customClassifier = new DbaasDbClassifier.Builder()
                .withProperty(SCOPE, DbaasConst.SERVICE)
                .withProperty(NAMESPACE, customNamespace)
                .withProperty(MICROSERVICE_NAME, customMicroserviceName)
                .build();

        Field fieldMicroserviceName = DatabasePool.class.getDeclaredField("microserviceName");
        Field fieldNamespace = DatabasePool.class.getDeclaredField("namespace");
        fieldMicroserviceName.setAccessible(true);
        fieldNamespace.setAccessible(true);
        Object valueMicroserviceName = fieldMicroserviceName.get(databasePool);
        Object valueNamespace = fieldNamespace.get(databasePool);
        assertNotEquals(customMicroserviceName, valueMicroserviceName);
        assertNotEquals(customNamespace, valueNamespace);

        Method method = DatabasePool.class.getDeclaredMethod("enrichClassifier", DbaasDbClassifier.class);
        method.setAccessible(true);
        method.invoke(databasePool, customClassifier);

        assertEquals(customClassifier.asMap().get(NAMESPACE), customNamespace);
        assertEquals(customClassifier.asMap().get(MICROSERVICE_NAME), customMicroserviceName);
    }

    private DatabasePool getDatabasePool() {
        return new DatabasePool(dbaasClient, TEST_MS_NAME, TEST_NAMESPACE, Collections.singletonList(postConnectProcessor), Mockito.mock(DatabaseDefinitionHandler.class));
    }

    @Test
    public void testGetOrCreateDatabaseOverloaded() {
        DatabasePool databasePool = getDatabasePool();

        final DatabaseSettings databaseSettings = new TestDatabaseSettings();

        TestDatabase receivedDatabase = databasePool.getOrCreateDatabase(TestDBType.INSTANCE, classifier, databaseConfigBuilder.databaseSettings(databaseSettings).build());
        assertEquals(testDatabase, receivedDatabase);
        Mockito.verify(postConnectProcessor, times(1)).process(testDatabase);

        Mockito.verify(dbaasClient, times(1)).getOrCreateDatabase(
                eq(TestDBType.INSTANCE),
                eq(TEST_NAMESPACE),
                anyMap(),
                any(DatabaseConfig.class));
        Mockito.verify(postConnectProcessor, times(1)).process(testDatabase);
    }

    @Test
    public void testGetOrCreateDatabaseEnrichClassifierCustomMicroserviceName() {
        DatabasePool databasePool = getDatabasePool();

        Map<String, Object> testClassifier = new TreeMap<>();
        testClassifier.put("microserviceName", "multiuserTest");
        testClassifier.put("scope", "service");
        DbaasDbClassifier testDbaasClassifier = new DbaasDbClassifier(testClassifier);


        databasePool.getOrCreateDatabase(TestDBType.INSTANCE, testDbaasClassifier,
                databaseConfigBuilder.databaseSettings(new TestDatabaseSettings()).build());


        testClassifier.put("namespace", TEST_NAMESPACE);

        Mockito.verify(dbaasClient, times(1)).getOrCreateDatabase(
                eq(TestDBType.INSTANCE),
                eq(TEST_NAMESPACE),
                eq(testClassifier),
                any(DatabaseConfig.class));
    }

    @Test
    public void testGetOrCreateDatabaseEnrichClassifier() {
        DatabasePool databasePool = getDatabasePool();

        Map<String, Object> testClassifier = new TreeMap<>();
        testClassifier.put("scope", "service");
        DbaasDbClassifier testDbaasClassifier = new DbaasDbClassifier(testClassifier);

        databasePool.getOrCreateDatabase(TestDBType.INSTANCE, testDbaasClassifier,
                databaseConfigBuilder.databaseSettings(new TestDatabaseSettings()).build());

        testClassifier.put("namespace", TEST_NAMESPACE);
        testClassifier.put("microserviceName", TEST_MS_NAME);


        Mockito.verify(dbaasClient, times(1)).getOrCreateDatabase(
                eq(TestDBType.INSTANCE),
                eq(TEST_NAMESPACE),
                eq(testClassifier),
                any(DatabaseConfig.class));
    }

    @Test
    public void testGetOrCreateDatabaseNullDbType() {
        assertThrows(NullPointerException.class,
                () -> {
                    DatabasePool databasePool = getDatabasePool();
                    databasePool.getOrCreateDatabase(null, classifier);
                });

    }

    @Test
    public void testGetOrCreateDatabaseNullClassifier() {
        assertThrows(NullPointerException.class,
                () -> {
                    DatabasePool databasePool = getDatabasePool();
                    databasePool.getOrCreateDatabase(TestDBType.INSTANCE, null);
                });
    }

    @Test
    public void testGetOrCreateDatabaseWithPhyDbId() {
        final String phyDbId = UUID.randomUUID().toString();

        DatabasePool databasePool = Mockito.spy(getDatabasePool());

        TestDatabase receivedDatabase = databasePool.getOrCreateDatabase(TestDBType.INSTANCE, classifier, databaseConfigBuilder.physicalDatabaseId(phyDbId).build());
        assertEquals(testDatabase, receivedDatabase);
        Mockito.verify(postConnectProcessor, times(1)).process(testDatabase);

        Mockito.verify(databasePool, times(1))
                .createDatabase(new DatabaseKey<>(TestDBType.INSTANCE, classifier.asMap(), null), databaseConfigBuilder.physicalDatabaseId(phyDbId).build());
    }

    @Test
    public void testGetDatabaseFromCache() {
        DatabasePool databasePool = getDatabasePool();

        TestDatabase receivedDatabase = databasePool.getOrCreateDatabase(TestDBType.INSTANCE, classifier);
        assertEquals(testDatabase, receivedDatabase);
        receivedDatabase = databasePool.getOrCreateDatabase(TestDBType.INSTANCE, classifier); // once again
        assertEquals(testDatabase, receivedDatabase);
        receivedDatabase = databasePool.getOrCreateDatabase(TestDBType.INSTANCE, classifier); // and once again
        assertEquals(testDatabase, receivedDatabase);

        Mockito.verify(dbaasClient, times(1)).getOrCreateDatabase(
                eq(TestDBType.INSTANCE),
                eq(TEST_NAMESPACE),
                anyMap(),
                any(DatabaseConfig.class));
        Mockito.verify(postConnectProcessor, times(1)).process(testDatabase);
    }

    @Test
    public void testGetDatabaseFromCacheWithDicriminator() {
        DatabasePool databasePool = getDatabasePool();
        TestConnectorSettings testSettings = new TestConnectorSettings("test-discriminator");

        TestDatabase receivedDatabase = databasePool.getOrCreateDatabase(TestDBType.INSTANCE, classifier, DatabaseConfig.builder().build(), testSettings);
        assertEquals(testDatabase, receivedDatabase);
        receivedDatabase = databasePool.getOrCreateDatabase(TestDBType.INSTANCE, classifier, DatabaseConfig.builder().build(), testSettings); // once again
        assertEquals(testDatabase, receivedDatabase);
        receivedDatabase = databasePool.getOrCreateDatabase(TestDBType.INSTANCE, classifier, DatabaseConfig.builder().build(), testSettings); // and once again
        assertEquals(testDatabase, receivedDatabase);

        Mockito.verify(dbaasClient, times(1)).getOrCreateDatabase(
                eq(TestDBType.INSTANCE),
                eq(TEST_NAMESPACE),
                anyMap(),
                any(DatabaseConfig.class));
        Mockito.verify(postConnectProcessor, times(1)).process(testDatabase);
    }

    @Test
    public void testGetDatabaseFromCacheWithDifferentDiscriminators() {
        DatabasePool databasePool = getDatabasePool();
        TestConnectorSettings testSettings = new TestConnectorSettings("first-discriminator");

        TestDatabase receivedDatabase = databasePool.getOrCreateDatabase(TestDBType.INSTANCE, classifier, DatabaseConfig.builder().build(), testSettings);
        assertEquals(testDatabase, receivedDatabase);
        receivedDatabase = databasePool.getOrCreateDatabase(TestDBType.INSTANCE, classifier, DatabaseConfig.builder().build(), testSettings); // once again
        assertEquals(testDatabase, receivedDatabase);

        databasePool.getOrCreateDatabase(TestDBType.INSTANCE, classifier, DatabaseConfig.builder().build(), new TestConnectorSettings("second-discriminator")); // and once again
        assertEquals(testDatabase, receivedDatabase);

        Mockito.verify(dbaasClient, times(2)).getOrCreateDatabase(
                eq(TestDBType.INSTANCE),
                eq(TEST_NAMESPACE),
                anyMap(),
                any(DatabaseConfig.class));
        Mockito.verify(postConnectProcessor, times(2)).process(testDatabase);
    }
    @Test
    public void testRemoveCachedDatabase() {
        DatabasePool databasePool = getDatabasePool();

        TestDatabase receivedDatabase = databasePool.getOrCreateDatabase(TestDBType.INSTANCE, classifier);
        assertEquals(testDatabase, receivedDatabase);

        databasePool.removeCachedDatabase(TestDBType.INSTANCE, classifier);

        receivedDatabase = databasePool.getOrCreateDatabase(TestDBType.INSTANCE, classifier); // get this db from DbaasClient again
        assertEquals(testDatabase, receivedDatabase);
        receivedDatabase = databasePool.getOrCreateDatabase(TestDBType.INSTANCE, classifier); // and now should get it from cache
        assertEquals(testDatabase, receivedDatabase);

        Mockito.verify(dbaasClient, times(2)).getOrCreateDatabase(
                eq(TestDBType.INSTANCE),
                eq(TEST_NAMESPACE),
                anyMap(),
                any(DatabaseConfig.class));
        Mockito.verify(postConnectProcessor, times(2)).process(testDatabase);
    }

    @Test
    public void testCacheOnPostConnectProcessorFailure() {
        DatabasePool databasePool = getDatabasePool();

        Mockito.doThrow(new RuntimeException("Expected test exception in postConnectProcessor"))
                .when(postConnectProcessor).process(any(TestDatabase.class));

        try {
            databasePool.getOrCreateDatabase(TestDBType.INSTANCE, classifier);
        } catch (RuntimeException e) {
            // ignore expected exception
        }
        try {
            databasePool.getOrCreateDatabase(TestDBType.INSTANCE, classifier); // now should get db from cache
        } catch (RuntimeException e) {
            // ignore expected exception
        }

        Mockito.verify(dbaasClient, times(1)).getOrCreateDatabase(
                eq(TestDBType.INSTANCE),
                eq(TEST_NAMESPACE),
                anyMap(),
                any(DatabaseConfig.class));
        Mockito.verify(postConnectProcessor, times(2)).process(testDatabase);
    }


    @Test
    public void provideDbByCustomLogicDbProvider() {
        String url = "custom_url", username = "custom_username", password = "custom_password";
        LogicalDbProvider<TestDBConnection, TestDatabase> customProvider = new LogicalDbProvider<TestDBConnection, TestDatabase>() {
            @Override
            public TestDatabase provide(SortedMap<String, Object> classifier, DatabaseConfig params, String namespace) {
                return getPostgresDatabase(url, username, password);
            }

            @Override
            public TestDBConnection provideConnectionProperty(SortedMap<String, Object> classifier, DatabaseConfig params) {
                return null;
            }

            @Override
            public Class<TestDatabase> getSupportedDatabaseType() {
                return TestDatabase.class;
            }
        };
        DatabasePool databasePool = new DatabasePool(dbaasClient, TEST_MS_NAME, TEST_NAMESPACE, Collections.singletonList(postConnectProcessor),
                Mockito.mock(DatabaseDefinitionHandler.class), null, Collections.singletonList(customProvider), null);
        TestDatabase testDatabase = databasePool.getOrCreateDatabase(TestDBType.INSTANCE, classifier);
        TestDBConnection connectionProperties = testDatabase.getConnectionProperties();
        assertEquals(url, connectionProperties.getUrl());
        assertEquals(username, connectionProperties.getUsername());
        assertEquals(password, connectionProperties.getPassword());
    }

    @Test
    public void getLogicalDbAfterCustomProviderReturnNull() {
        LogicalDbProvider<TestDBConnection, TestDatabase> customProvider = new LogicalDbProvider<TestDBConnection, TestDatabase>() {
            @Override
            public int order() {
                return Integer.MIN_VALUE;
            }

            @Override
            public TestDBConnection provideConnectionProperty(SortedMap<String, Object> classifier, DatabaseConfig params) {
                return null;
            }

            @Override
            public TestDatabase provide(SortedMap<String, Object> classifier, DatabaseConfig params, String namespace) {
                return null;
            }

            @Override
            public Class<TestDatabase> getSupportedDatabaseType() {
                return TestDatabase.class;
            }
        };
        String url = "pg-url", username = "username", password = "pwd";
        doReturn(getPostgresDatabase(url, username, password)).when(dbaasClient).getOrCreateDatabase(any(), anyString(), any(), any(DatabaseConfig.class));
        DatabasePool databasePool = new DatabasePool(dbaasClient, TEST_MS_NAME, TEST_NAMESPACE, Collections.singletonList(postConnectProcessor),
                Mockito.mock(DatabaseDefinitionHandler.class), null, Arrays.asList(new TestLogicalDbProvider(dbaasClient), customProvider), null);

        TestDatabase testDatabase = databasePool.getOrCreateDatabase(TestDBType.INSTANCE, classifier);

        TestDBConnection connectionProperties = testDatabase.getConnectionProperties();
        assertEquals(url, connectionProperties.getUrl());
        assertEquals(username, connectionProperties.getUsername());
        assertEquals(password, connectionProperties.getPassword());
    }

    @Test
    public void checkLogicalDbProviderChache() {
        LogicalDbProvider<TestDBConnection, TestDatabase> customProvider = new LogicalDbProvider<TestDBConnection, TestDatabase>() {
            @Override
            public TestDatabase provide(SortedMap<String, Object> classifier, DatabaseConfig params, String namespace) {
                return null;
            }

            @Override
            public TestDBConnection provideConnectionProperty(SortedMap<String, Object> classifier, DatabaseConfig params) {
                return null;
            }

            @Override
            public Class<TestDatabase> getSupportedDatabaseType() {
                return TestDatabase.class;
            }
        };
        String url = "pg-url", username = "username", password = "pwd";
        doReturn(getPostgresDatabase(url, username, password)).when(dbaasClient).getOrCreateDatabase(any(), anyString(), any(), any(DatabaseConfig.class));
        DatabasePool databasePool = new DatabasePool(dbaasClient, TEST_MS_NAME, TEST_NAMESPACE, Collections.singletonList(postConnectProcessor),
                Mockito.mock(DatabaseDefinitionHandler.class), null, Arrays.asList(new TestLogicalDbProvider(dbaasClient), customProvider), null);

        databasePool.getOrCreateDatabase(TestDBType.INSTANCE, classifier);
        databasePool.getOrCreateDatabase(TestDBType.INSTANCE, classifier);

        verify(dbaasClient, Mockito.times(1)).getOrCreateDatabase(eq(TestDBType.INSTANCE),
                eq(TEST_NAMESPACE),
                anyMap(),
                any(DatabaseConfig.class));
    }


    private TestDatabase getPostgresDatabase(String url, String username, String password) {
        TestDatabase testDatabase = new TestDatabase();
        testDatabase.setConnectionProperties(new TestDBConnection(url, username, password));
        return testDatabase;
    }
}
