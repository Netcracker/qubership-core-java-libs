package com.netcracker.cloud.quarkus.dbaas.mongoclient.service.impl;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.netcracker.cloud.dbaas.client.DbaaSClientOkHttpImpl;
import com.netcracker.cloud.dbaas.client.DbaasClient;
import com.netcracker.cloud.dbaas.client.entity.DbaasApiProperties;
import com.netcracker.cloud.dbaas.client.management.DatabaseConfig;
import com.netcracker.cloud.dbaas.client.management.DbaasDbClassifier;
import com.netcracker.cloud.dbaas.common.config.DbaasApiPropertiesConfig;
import com.netcracker.cloud.quarkus.dbaas.mongoclient.config.properties.DbaasMongoDbCreationConfig;
import com.netcracker.cloud.quarkus.dbaas.mongoclient.config.properties.MongoDbConfiguration;
import com.netcracker.cloud.quarkus.dbaas.mongoclient.entity.connection.MongoDBConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.netcracker.cloud.dbaas.client.DbaasConst.SCOPE;
import static com.netcracker.cloud.dbaas.client.DbaasConst.SERVICE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class ContainerMongoClientSchemasTest extends ContainerMongoDbBaseConfig {

    private MongoClient mongo;

    private static com.netcracker.cloud.quarkus.dbaas.mongoclient.entity.database.MongoDatabase mongoDatabase;
    private static MongoClientCreationImpl mongoClientCreationImpl;
    private static DbaasClient dbaaSClient;

    @BeforeEach
    public void createDb() {
        DbaasMongoDbCreationConfig dbaasMongoDbCreationConfig = mock(DbaasMongoDbCreationConfig.class);
        DbaasApiPropertiesConfig dbaasApiPropertiesConfig = mock(DbaasApiPropertiesConfig.class);
        DbaasApiProperties dbaasApiProperties = new DbaasApiProperties();
        when(dbaasApiPropertiesConfig.getDbaaseApiProperties()).thenReturn(dbaasApiProperties);
        when(dbaasMongoDbCreationConfig.dbaasApiPropertiesConfig()).thenReturn(dbaasApiPropertiesConfig);
        when(dbaasMongoDbCreationConfig.getMongoDbConfiguration(any())).thenReturn(new MongoDbConfiguration(Optional.empty()));

        mongoClientCreationImpl = new MongoClientCreationImpl(dbaasMongoDbCreationConfig);
        MongoDBConnection mongoDBConnection = new MongoDBConnection();
        mongoDBConnection.setUsername(USERNAME);
        mongoDBConnection.setPassword(PASSWORD);
        mongoDBConnection.setUrl(URL);
        mongoDBConnection.setAuthDbName(DATABASE);

        mongoClientCreationImpl.namespace = "test-namespace";
        mongoDatabase = new com.netcracker.cloud.quarkus.dbaas.mongoclient.entity.database.MongoDatabase();
        mongoDatabase.setConnectionProperties(mongoDBConnection);
        mongoDatabase.setName(DATABASE);

        dbaaSClient = mock(DbaaSClientOkHttpImpl.class);
        when(dbaaSClient.getOrCreateDatabase(any(), anyString(), anyMap(), any(DatabaseConfig.class))).thenReturn(mongoDatabase);
        mongoClientCreationImpl.dbaaSClient = dbaaSClient;

        Map<String, Object> params = new HashMap<>();
        params.put("microserviceName", "test-service");
        params.put("dbClassifier", "default");
        params.put(SCOPE, SERVICE);
        DbaasDbClassifier classifier = new DbaasDbClassifier(params);
        com.netcracker.cloud.quarkus.dbaas.mongoclient.entity.database.MongoDatabase db = mongoClientCreationImpl.getOrCreateMongoDatabase(classifier);

        mongo = db.getConnectionProperties().getClient();
    }


    @Test
    void testInsertEntitiesWithUUIDAndStringId() {
        MongoDatabase db = mongo.getDatabase(DATABASE);

        db.createCollection("testCollection");

        MongoCollection<TestEntityWithUUID> col = db.getCollection("testCollection", TestEntityWithUUID.class);
        MongoCollection<TestEntityWithStringId> col2 = db.getCollection("testCollection", TestEntityWithStringId.class);

        col.insertOne(new TestEntityWithUUID(UUID.randomUUID(), "testEntity"));

        assertEquals(1L, col.countDocuments());

        col2.insertOne(new TestEntityWithStringId("asdA23DAS3", "testEntity"));

        assertEquals(2L, col.countDocuments());
    }

}
