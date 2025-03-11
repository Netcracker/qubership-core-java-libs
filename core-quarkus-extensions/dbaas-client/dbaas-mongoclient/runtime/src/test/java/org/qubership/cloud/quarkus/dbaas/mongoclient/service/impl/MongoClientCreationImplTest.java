package org.qubership.cloud.quarkus.dbaas.mongoclient.service.impl;

import org.qubership.cloud.dbaas.client.DbaaSClientOkHttpImpl;
import org.qubership.cloud.dbaas.client.DbaasClient;
import org.qubership.cloud.dbaas.client.entity.DbaasApiProperties;
import org.qubership.cloud.dbaas.client.management.DatabaseConfig;
import org.qubership.cloud.dbaas.client.management.DbaasDbClassifier;
import org.qubership.cloud.dbaas.common.config.DbaasApiPropertiesConfig;
import org.qubership.cloud.quarkus.dbaas.mongoclient.config.MongoClientConfiguration;
import org.qubership.cloud.quarkus.dbaas.mongoclient.config.properties.DbaasMongoDbCreationConfig;
import org.qubership.cloud.quarkus.dbaas.mongoclient.entity.connection.MongoDBConnection;
import org.qubership.cloud.quarkus.dbaas.mongoclient.entity.database.MongoDatabase;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.qubership.cloud.dbaas.client.DbaasConst.SCOPE;
import static org.qubership.cloud.dbaas.client.DbaasConst.SERVICE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

class MongoClientCreationImplTest {
    private static MongoClientCreationImpl mongoClientCreationImpl;
    private static DbaasClient dbaaSClient;
    private static MongoDatabase mongoDatabase;
    public static final String USER_ROLE = "quarkus.dbaas.mongo.api.runtime-user-role";

    @BeforeAll
    static void prepare() {
        DbaasMongoDbCreationConfig dbaasMongoDbCreationConfig = new DbaasMongoDbCreationConfig();
        DbaasApiPropertiesConfig dbaasApiPropertiesConfig = mock(DbaasApiPropertiesConfig.class);
        DbaasApiProperties dbaasApiProperties = new DbaasApiProperties();
        when(dbaasApiPropertiesConfig.getDbaaseApiProperties()).thenReturn(dbaasApiProperties);
        dbaasMongoDbCreationConfig.dbaasApiPropertiesConfig = dbaasApiPropertiesConfig;

        mongoClientCreationImpl = new MongoClientCreationImpl(dbaasMongoDbCreationConfig);
        mongoClientCreationImpl.namespace = "test-namespace";
        MongoDBConnection mongoDBConnection = new MongoDBConnection();
        mongoDBConnection.setUrl("test-url");
        mongoDBConnection.setUsername("test-username");
        mongoDBConnection.setPassword("test-password");
        mongoDBConnection.setUrl("mongodb://admin");
        mongoDBConnection.setAuthDbName("dbName");

        mongoDatabase = new MongoDatabase();
        mongoDatabase.setConnectionProperties(mongoDBConnection);

        dbaaSClient = mock(DbaaSClientOkHttpImpl.class);
        when(dbaaSClient.getOrCreateDatabase(any(), anyString(), anyMap(), any(DatabaseConfig.class))).thenReturn(mongoDatabase);
        mongoClientCreationImpl.dbaaSClient = dbaaSClient;
    }

    @Test
    void mustCreateMongoDatabaseByClassifier() {
        Map<String, Object> params = new HashMap<>();
        params.put("microserviceName", "test-service");
        params.put("dbClassifier", "default");
        params.put(SCOPE, SERVICE);
        DbaasDbClassifier classifier = new DbaasDbClassifier(params);
        MongoDatabase db = mongoClientCreationImpl.getOrCreateMongoDatabase(classifier);
        assertNotNull(db);
        assertNotNull(db.getConnectionProperties().getClient());
        assertEquals("dbName", db.getConnectionProperties().getAuthDbName());
    }

    @Test
    void testCorrectBaseClassifierCreation() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        MongoClientConfiguration mongoClientConfiguration = new MongoClientConfiguration();
        Method method = mongoClientConfiguration.getClass().getDeclaredMethod("getInitialClassifierMap");
        method.setAccessible(true);

        Field namespace = mongoClientConfiguration.getClass().getDeclaredField("namespace");
        namespace.setAccessible(true);
        namespace.set(mongoClientConfiguration, "test-namespace");

        Field microserviceName = mongoClientConfiguration.getClass().getDeclaredField("microserviceName");
        microserviceName.setAccessible(true);
        microserviceName.set(mongoClientConfiguration, "test-microserviceName");

        Field cassandraProperties = mongoClientConfiguration.getClass().getDeclaredField("dbClassifierField");
        cassandraProperties.setAccessible(true);
        cassandraProperties.set(mongoClientConfiguration, "test");

        Map<String, Object> classifeir = new HashMap<>();
        classifeir.put("dbClassifier", "test");
        classifeir.put("microserviceName", "test-microserviceName");
        classifeir.put("namespace", "test-namespace");
        assertEquals(classifeir, method.invoke(mongoClientConfiguration));
    }


    @QuarkusTest
    @TestProfile(MongoClientCreationImplTest.ExactRoleTestProfile.class)
    public static class WithDbPrefixAndRoleTest {

        @Test
        void mustContainsUserRole() {
            Map<String, Object> params = new HashMap<>();
            params.put("microserviceName", "test-service");
            params.put("dbClassifier", "default");
            params.put(SCOPE, SERVICE);
            DbaasDbClassifier classifier = new DbaasDbClassifier(params);

            DatabaseConfig databaseConfig = DatabaseConfig.builder().userRole("admin").build();
            mongoClientCreationImpl.getOrCreateMongoDatabase(classifier);
            Mockito.verify(dbaaSClient, times(1)).getOrCreateDatabase(any(), anyString(), anyMap(), eq(databaseConfig));
        }
    }

    @NoArgsConstructor
    protected static final class ExactRoleTestProfile implements QuarkusTestProfile {
        protected static final String USER_ROLE_VAL = "admin";

        @Override
        public Map<String, String> getConfigOverrides() {
            Map<String, String> properties = new HashMap<>();
            properties.put(USER_ROLE, USER_ROLE_VAL);
            return properties;
        }
    }

}