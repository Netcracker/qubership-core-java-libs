package com.netcracker.cloud.quarkus.dbaas.mongoclient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mongodb.client.MongoClient;
import com.netcracker.cloud.dbaas.client.DbaasClient;
import com.netcracker.cloud.dbaas.client.entity.DatabaseCreateRequest;
import com.netcracker.cloud.dbaas.client.entity.MongoDatabaseSettings;
import com.netcracker.cloud.dbaas.client.entity.database.type.DatabaseType;
import com.netcracker.cloud.dbaas.client.management.DatabaseConfig;
import com.netcracker.cloud.quarkus.dbaas.mongoclient.config.properties.DbaasMongoDbCreationConfig;
import com.netcracker.cloud.quarkus.dbaas.mongoclient.entity.connection.MongoDBConnection;
import com.netcracker.cloud.quarkus.dbaas.mongoclient.entity.database.MongoDatabase;
import io.quarkus.arc.DefaultBean;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.netcracker.cloud.quarkus.dbaas.mongoclient.MongoConfigTest.ExactDbPrefixAndRoleTestProfile.*;
import static com.netcracker.cloud.quarkus.dbaas.mongoclient.config.MongoClientConfiguration.SERVICE_MONGO_CLIENT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@QuarkusTest
@TestProfile(MongoConfigTest.ExactDbPrefixAndRoleTestProfile.class)
public class MongoConfigTest {
    private static ArgumentCaptor<DatabaseConfig> databaseCreateRequestArgumentCaptor;

    @Inject
    DbaasMongoDbCreationConfig config;

    @Inject
    @Named(SERVICE_MONGO_CLIENT)
    DbaaSMongoClient client;

    @Test
    public void testConfigs() {
        Optional<String> dbPrefix = config.dbaasApiPropertiesConfig().dbPrefix();
        Optional<String> userRole = config.dbaasApiPropertiesConfig().runtimeUserRole();

        MongoDatabaseSettings serviceSettings = config.getMongoDbConfiguration(null).getDatabaseSettings().orElse(null);
        MongoDatabaseSettings allTenantsSettings = config.getMongoDbConfiguration("default").getDatabaseSettings().orElse(null);
        MongoDatabaseSettings specificTenantSettings = config.getMongoDbConfiguration(SPECIFIC_TENANT_NAME).getDatabaseSettings().orElse(null);
        Assertions.assertNotNull(dbPrefix);
        Assertions.assertNotNull(userRole);
        Assertions.assertNotNull(serviceSettings);
        Assertions.assertNotNull(allTenantsSettings);
        Assertions.assertNotNull(specificTenantSettings);
        Assertions.assertEquals("test-prefix", dbPrefix.get());
        Assertions.assertEquals("admin", userRole.get());
        Assertions.assertEquals(SERVICE_SHARDING_VALUE, serviceSettings.getShardingSettings().toString());
        Assertions.assertEquals(SERVICE_TARGET_SHARD_VALUE, serviceSettings.getTargetShard());
        Assertions.assertEquals(ALL_TENANTS_SHARDING_VALUE, allTenantsSettings.getShardingSettings().toString());
        Assertions.assertEquals(ALL_TENANTS_TARGET_SHARD_VALUE, allTenantsSettings.getTargetShard());
        Assertions.assertEquals(SPECIFIC_TENANT_SHARDING_VALUE, specificTenantSettings.getShardingSettings().toString());
        Assertions.assertEquals(SPECIFIC_TENANT_TARGET_SHARD_VALUE, specificTenantSettings.getTargetShard());

        client.getOrCreateMongoDb();
        DatabaseConfig value = databaseCreateRequestArgumentCaptor.getValue();
        Assertions.assertEquals(SERVICE_SHARDING_VALUE, ((MongoDatabaseSettings) value.getDatabaseSettings()).getShardingSettings().toString());
        Assertions.assertEquals(SERVICE_TARGET_SHARD_VALUE, ((MongoDatabaseSettings) value.getDatabaseSettings()).getTargetShard());
    }

    @Test
    public void testSerialiseSettings() throws JsonProcessingException {
        Map<String, Object> classifier = Map.of("microserviceName", "test-ms");
        MongoDatabaseSettings mongoDatabaseSettings = config.getMongoDbConfiguration(null).getDatabaseSettings().get();
        String expectedSettings = ",\"settings\":{\"shardingSettings\":[{\"collectionName\":\"serviceCollection\",\"shardKey\":\"key1\",\"strategy\":\"hashed\"}],\"targetShard\":\"service-shard\"},";

        DatabaseConfig databaseConfig = DatabaseConfig.builder().databaseSettings(mongoDatabaseSettings).build();
        DatabaseCreateRequest databaseCreateRequest = new DatabaseCreateRequest(classifier, "mongodb", databaseConfig);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        String databaseCreateRequestJson = objectMapper.writeValueAsString(databaseCreateRequest);
        Assertions.assertTrue(databaseCreateRequestJson.contains(expectedSettings));
    }

    private static Map<String, String> getBaseProperties() {
        Map<String, String> properties = new HashMap<>();
        properties.put("quarkus.datasource.devservices", "false");
        properties.put("cloud.microservice.name", "dbaas-client-mongo-params-test");
        properties.put("cloud.microservice.namespace", "test-namespace");
        properties.put("quarkus.http.test-port", "0");
        properties.put("quarkus.http.test-ssl-port", "0");
        return properties;
    }

    @NoArgsConstructor
    protected static final class ExactDbPrefixAndRoleTestProfile implements QuarkusTestProfile {
        private static final String DB_NAME_PREFIX = "quarkus.dbaas.mongo.api.db-prefix";
        private static final String USER_ROLE = "quarkus.dbaas.mongo.api.runtime-user-role";
        protected static final String DB_PREFIX_PROPERTY_VAL = "test-prefix";
        protected static final String USER_ROLE_VAL = "admin";
        protected static final String SERVICE_SHARDING_VALUE = "[{\"collectionName\":\"serviceCollection\",\"shardKey\":\"key1\",\"strategy\":\"hashed\"}]";
        protected static final String SERVICE_TARGET_SHARD_VALUE = "service-shard";
        protected static final String ALL_TENANTS_SHARDING_VALUE = "[{\"collectionName\":\"allTenantCollection1\",\"shardKey\":\"key2\",\"strategy\":\"ranged\"}," +
                "{\"collectionName\":\"allTenantCollection2\",\"shardKey\":\"key2\",\"strategy\":\"hashed\"}]";
        protected static final String ALL_TENANTS_TARGET_SHARD_VALUE = "all-tenants-shard";
        protected static final String SPECIFIC_TENANT_NAME = "specific-tenant";
        protected static final String SPECIFIC_TENANT_SHARDING_VALUE = "[{\"collectionName\":\"specificTenantCollection\",\"shardKey\":\"key3\",\"strategy\":\"hashed\"}]";
        protected static final String SPECIFIC_TENANT_TARGET_SHARD_VALUE = "specific-tenant-shard";

        @Override
        public Map<String, String> getConfigOverrides() {
            Map<String, String> properties = getBaseProperties();
            properties.put(DB_NAME_PREFIX, DB_PREFIX_PROPERTY_VAL);
            properties.put(USER_ROLE, USER_ROLE_VAL);
            properties.put("quarkus.dbaas.mongo.api.service.database-settings.sharding-settings", SERVICE_SHARDING_VALUE);
            properties.put("quarkus.dbaas.mongo.api.service.database-settings.target-shard", SERVICE_TARGET_SHARD_VALUE);
            properties.put("quarkus.dbaas.mongo.api.tenant.database-settings.sharding-settings", ALL_TENANTS_SHARDING_VALUE);
            properties.put("quarkus.dbaas.mongo.api.tenant.database-settings.target-shard", ALL_TENANTS_TARGET_SHARD_VALUE);
            properties.put("quarkus.dbaas.mongo.api.tenant." + SPECIFIC_TENANT_NAME + ".database-settings.sharding-settings", SPECIFIC_TENANT_SHARDING_VALUE);
            properties.put("quarkus.dbaas.mongo.api.tenant." + SPECIFIC_TENANT_NAME + ".database-settings.target-shard", SPECIFIC_TENANT_TARGET_SHARD_VALUE);
            return properties;
        }
    }

    @Produces
    @DefaultBean
    public AnnotationParsingBean getAnnotationParsingBeanStub() {
        return new AnnotationParsingBean(List.of(), List.of());
    }

    @Produces
    public DbaasClient dbaaSClient() {
        databaseCreateRequestArgumentCaptor = ArgumentCaptor.forClass(DatabaseConfig.class);
        MongoDatabase mongoDatabase = Mockito.mock(MongoDatabase.class);
        MongoDBConnection mongoDBConnection = Mockito.mock(MongoDBConnection.class);
        Mockito.when(mongoDBConnection.getClient()).thenReturn(Mockito.mock(MongoClient.class));
        Mockito.when(mongoDBConnection.getUrl()).thenReturn("mongodb://test");
        Mockito.when(mongoDBConnection.getAuthDbName()).thenReturn("dbname");
        Mockito.when(mongoDBConnection.getUsername()).thenReturn("user");
        Mockito.when(mongoDBConnection.getPassword()).thenReturn("password");
        Mockito.when(mongoDatabase.getConnectionProperties()).thenReturn(mongoDBConnection);
        DbaasClient mock = Mockito.mock(DbaasClient.class);
        Mockito.when(mock.getOrCreateDatabase(any(DatabaseType.class), anyString(), any(), databaseCreateRequestArgumentCaptor.capture()))
                .thenReturn(mongoDatabase);
        return mock;
    }
}
