package com.netcracker.cloud.dbaas.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mongodb.client.MongoClient;
import com.netcracker.cloud.context.propagation.core.ContextManager;
import com.netcracker.cloud.dbaas.client.config.EnableDbaasMongo;
import com.netcracker.cloud.dbaas.client.entity.DatabaseCreateRequest;
import com.netcracker.cloud.dbaas.client.entity.DbaasApiProperties;
import com.netcracker.cloud.dbaas.client.entity.MongoDatabaseSettings;
import com.netcracker.cloud.dbaas.client.entity.connection.MongoDBConnection;
import com.netcracker.cloud.dbaas.client.entity.database.DatabaseSettings;
import com.netcracker.cloud.dbaas.client.entity.database.MongoDatabase;
import com.netcracker.cloud.dbaas.client.entity.database.type.DatabaseType;
import com.netcracker.cloud.dbaas.client.management.DatabaseConfig;
import com.netcracker.cloud.dbaas.client.management.DatabasePool;
import com.netcracker.cloud.dbaas.client.management.DbaasDbClassifier;
import com.netcracker.cloud.framework.contexts.tenant.TenantContextObject;
import com.netcracker.cloud.restclient.MicroserviceRestClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Map;

import static com.netcracker.cloud.dbaas.client.config.DbaasMongoConfiguration.SERVICE_MONGO_TEMPLATE;
import static com.netcracker.cloud.dbaas.client.config.DbaasMongoConfiguration.TENANT_MONGO_TEMPLATE;
import static com.netcracker.cloud.dbaas.client.entity.MongoDatabaseSettings.SHARDING_PROPERTY_NAME;
import static com.netcracker.cloud.dbaas.client.entity.MongoDatabaseSettings.TARGET_SHARD_PROPERTY_NAME;
import static com.netcracker.cloud.framework.contexts.tenant.BaseTenantProvider.TENANT_CONTEXT_NAME;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {MongoApiPropertiesTest.TestConfig.class},
        properties = {
                "dbaas.api.mongo.db-prefix=test-prefix",
                "dbaas.api.mongo.runtime-user-role=test-role",
                "dbaas.api.mongo.service.database-settings." + SHARDING_PROPERTY_NAME + "=[{\"collectionName\": \"serviceCollection\",\"shardKey\": \"key1\",\"strategy\": \"hashed\"}]",
                "dbaas.api.mongo.tenant.database-settings." + SHARDING_PROPERTY_NAME + "=[{\"collectionName\": \"tenantCollection\",\"shardKey\": \"key2\",\"strategy\": \"hashed\"}]",
                "dbaas.api.mongo.tenant.database-settings." + TARGET_SHARD_PROPERTY_NAME + "=shardName"
        })
public class MongoApiPropertiesTest {
    private ArgumentCaptor<DatabaseConfig> databaseCreateRequestArgumentCaptor;

    @Autowired
    @Qualifier(SERVICE_MONGO_TEMPLATE)
    private MongoTemplate serviceTemplate;

    @Autowired
    @Qualifier(TENANT_MONGO_TEMPLATE)
    private MongoTemplate tenantTemplate;

    @Autowired
    private DatabasePool databasePool;

    @Autowired
    private DbaasApiProperties mongoDbaasApiProperties;

    @BeforeEach
    void setUp() {
        Mockito.reset(databasePool);
        databaseCreateRequestArgumentCaptor = ArgumentCaptor.forClass(DatabaseConfig.class);
        MongoDatabase mongoDatabase = Mockito.mock(MongoDatabase.class);
        MongoDBConnection mongoDBConnection = Mockito.mock(MongoDBConnection.class);
        Mockito.when(mongoDBConnection.getClient()).thenReturn(Mockito.mock(MongoClient.class));
        Mockito.when(mongoDatabase.getConnectionProperties()).thenReturn(mongoDBConnection);
        Mockito.when(databasePool.getOrCreateDatabase(any(DatabaseType.class), any(DbaasDbClassifier.class), databaseCreateRequestArgumentCaptor.capture()))
                .thenReturn(mongoDatabase);
    }

    @Test
    public void checkMongoApiProperties() {
        Assertions.assertEquals("test-prefix", mongoDbaasApiProperties.getDbPrefix());
        Assertions.assertEquals("test-role", mongoDbaasApiProperties.getRuntimeUserRole());
    }

    @Test
    public void testServiceConfigs() {
        serviceTemplate.getDb();
        DatabaseConfig databaseConfig = databaseCreateRequestArgumentCaptor.getValue();
        DatabaseSettings databaseSettings = databaseConfig.getDatabaseSettings();
        Assertions.assertNotNull(databaseSettings);
        Assertions.assertTrue(((MongoDatabaseSettings) databaseSettings).getShardingSettings().toString().contains("serviceCollection"));
        Assertions.assertNull(((MongoDatabaseSettings) databaseSettings).getTargetShard());
    }

    @Test
    public void testTenantConfigs() {
        ContextManager.set(TENANT_CONTEXT_NAME, new TenantContextObject("test-tenant"));
        tenantTemplate.getDb();
        DatabaseConfig databaseConfig = databaseCreateRequestArgumentCaptor.getValue();
        DatabaseSettings databaseSettings = databaseConfig.getDatabaseSettings();
        Assertions.assertNotNull(databaseSettings);
        Assertions.assertTrue(((MongoDatabaseSettings) databaseSettings).getShardingSettings().toString().contains("tenantCollection"));
        Assertions.assertTrue(((MongoDatabaseSettings) databaseSettings).getTargetShard().contains("shardName"));
    }

    @Test
    public void testSerialiseSettings() throws JsonProcessingException {
        Map<String, Object> classifier = Map.of("microserviceName", "test-ms");
        MongoDatabaseSettings mongoDatabaseSettings = new MongoDatabaseSettings(mongoDbaasApiProperties.getDatabaseSettings(DbaasApiProperties.DbScope.TENANT));
        String expectedSettings = ",\"settings\":{\"shardingSettings\":[{\"collectionName\":\"tenantCollection\",\"shardKey\":\"key2\",\"strategy\":\"hashed\"}],\"targetShard\":\"shardName\"},";

        DatabaseConfig databaseConfig = DatabaseConfig.builder().databaseSettings(mongoDatabaseSettings).build();
        DatabaseCreateRequest databaseCreateRequest = new DatabaseCreateRequest(classifier, "mongodb", databaseConfig);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        String databaseCreateRequestJson = objectMapper.writeValueAsString(databaseCreateRequest);
        Assertions.assertTrue(databaseCreateRequestJson.contains(expectedSettings));
    }

    @Test
    public void testIncorrectShardingSettings() {
        Assertions.assertThrows(RuntimeException.class, () -> new MongoDatabaseSettings(Map.of(SHARDING_PROPERTY_NAME, "notJson")));
    }

    @Configuration
    @EnableDbaasMongo
    public static class TestConfig {
        @Primary
        @Bean
        @Qualifier("dbaasRestClient")
        public static MicroserviceRestClient microserviceRestClient() {
            return Mockito.mock(MicroserviceRestClient.class);
        }

        @Primary
        @Bean
        public DatabasePool dbaasConnectionPoolMock() {
            return Mockito.mock(DatabasePool.class);
        }
    }
}
