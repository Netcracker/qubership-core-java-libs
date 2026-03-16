package com.netcracker.cloud.dbaas.client.config.msframeworkspecific;

import com.mongodb.client.MongoClient;
import com.netcracker.cloud.dbaas.client.config.msframeworkspecific.testconfig.TestMongoDbConfiguration;
import com.netcracker.cloud.dbaas.client.entity.connection.MongoDBConnection;
import com.netcracker.cloud.dbaas.client.entity.database.MongoDatabase;
import com.netcracker.cloud.dbaas.client.management.DatabaseConfig;
import com.netcracker.cloud.dbaas.client.management.DatabasePool;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.TreeMap;

import static com.netcracker.cloud.dbaas.client.config.DbaasMongoConfiguration.SERVICE_MONGO_DB_FACTORY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static com.netcracker.cloud.dbaas.client.config.msframeworkspecific.testconfig.TestMongoDbConfiguration.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {TestMongoDbConfiguration.class}, properties = "dbaas.api.mongo.runtime-user-role=admin")
public class MongoDbFactoryTest {

    @Autowired
    @Qualifier(SERVICE_MONGO_DB_FACTORY)
    MongoDatabaseFactory mongoDatabaseFactory;

    @Autowired
    private DatabasePool databasePool;

    @Test
    public void testPool() {
        Assertions.assertNotNull(databasePool);
    }

    @Autowired
    private MongoClient mongoClient;

    @BeforeEach
    public void setUp() throws Exception {

        MongoDatabase database = new MongoDatabase();
        database.setName(TARGET_DB_NAME);

        MongoDBConnection connection = new MongoDBConnection();
        connection.setUrl(MONGO_TEST_URI);
        connection.setUsername(DB_USER);
        connection.setAuthDbName(DB_NAME);
        connection.setPassword(DB_PASSWORD);
        connection.setClient(mongoClient);
        database.setConnectionProperties(connection);
        database.setClassifier(new TreeMap<>());

        when(databasePool.getOrCreateDatabase(any(), any(), any())).thenReturn(database);
    }

    @Test
    public void testGetDb_Factory() throws Exception {
        com.mongodb.client.MongoDatabase mongoDb = mongoDatabaseFactory.getMongoDatabase();
        Assertions.assertEquals(TARGET_DB_NAME, mongoDb.getName());
        DatabaseConfig databaseConfig = DatabaseConfig.builder().userRole("admin").build();
        Mockito.verify(databasePool, times(1)).getOrCreateDatabase(any(), any(), eq(databaseConfig));
    }
}
