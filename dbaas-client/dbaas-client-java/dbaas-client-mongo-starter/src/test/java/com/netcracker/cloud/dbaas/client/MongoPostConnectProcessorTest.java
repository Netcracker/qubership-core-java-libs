package com.netcracker.cloud.dbaas.client;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.internal.MongoClientImpl;
import org.qubership.cloud.dbaas.client.entity.connection.MongoDBConnection;
import org.qubership.cloud.dbaas.client.entity.database.MongoDatabase;
import org.qubership.cloud.dbaas.client.management.MongoPostConnectProcessor;
import org.qubership.cloud.dbaas.client.test.configuration.TestMongoConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertThrows;

/*
*         private int minConnectionsPerHost;
private int maxConnectionsPerHost;
*/
@SpringBootTest
@ContextConfiguration(classes = {TestMongoConfiguration.class})
@TestPropertySource(properties = {
        "dbaas.mongo.options.applicationName=test application",
        "dbaas.mongo.options.minConnectionsPerHost=27",
        "dbaas.mongo.options.maxConnectionsPerHost=31",
        "dbaas.mongo.options.serverSelectionTimeout=35",
        "dbaas.mongo.options.maxWaitTime=37",
        "dbaas.mongo.options.maxConnectionIdleTime=39",
        "dbaas.mongo.options.maxConnectionLifeTime=41",
        "dbaas.mongo.options.connectTimeout=43",
        "dbaas.mongo.options.socketTimeout=45",
        "dbaas.mongo.options.sslEnabled=true",
        "dbaas.mongo.options.sslInvalidHostNameAllowed=true",
        "dbaas.mongo.options.minHeartbeatFrequency=53",
        "dbaas.mongo.options.heartbeatFrequency=55",
        "dbaas.mongo.options.heartbeatConnectTimeout=57",
        "dbaas.mongo.options.heartbeatSocketTimeout=59",
        "dbaas.mongo.options.localThreshold=61",
        "dbaas.mongo.options.requiredReplicaSetName=test replica set name",
        "dbaas.mongo.options.cursorFinalizerEnabled=true"
})
public class MongoPostConnectProcessorTest {
    public static String mongoTestHost = "localhost";
    public static int mongoTestPort = 27017;
    public static String mongoTestDbName = "authdb";

    @Autowired
    private MongoPostConnectProcessor mongoPostConnectProcessor;

    @BeforeEach
    public void init() {
    }

    @Test
    public void testMongoOptionsAreApplied() {
        MongoDatabase database = new MongoDatabase();
        database.setName(mongoTestDbName);
        MongoDBConnection connection = new MongoDBConnection();
        connection.setUrl("mongodb://mongo:mongo@" + mongoTestHost + ":" + mongoTestPort + "/" + mongoTestDbName);
        connection.setUsername("mongo");
        connection.setPassword("mongo");
        database.setConnectionProperties(connection);

        mongoPostConnectProcessor.process(database);

        MongoClientImpl mongoClient = (MongoClientImpl) database.getConnectionProperties().getClient();
        MongoClientSettings mongoClientSettings = mongoClient.getSettings();
        Assertions.assertNotNull(mongoClientSettings);
        Assertions.assertEquals(5, mongoClientSettings.getConnectionPoolSettings().getMaxConnectionIdleTime(TimeUnit.MINUTES));
        Assertions.assertEquals(5, mongoClientSettings.getConnectionPoolSettings().getMaxConnectionLifeTime(TimeUnit.MINUTES));
    }

    @Test
    public void testNormalPassword_NoPasswordProvider() {
        String password = "mongo-password";
        MongoDatabase database = new MongoDatabase();
        database.setName(mongoTestDbName);
        MongoDBConnection connection = new MongoDBConnection();
        connection.setUrl("mongodb://mongo:" + password + "@" + mongoTestHost + ":" + mongoTestPort + "/" + mongoTestDbName);
        connection.setUsername("mongo");
        connection.setPassword(password);
        database.setConnectionProperties(connection);

        new MongoPostConnectProcessor( null).process(database);

        MongoClient mongoClient = database.getConnectionProperties().getClient();
        Assertions.assertNotNull(mongoClient);
        Assertions.assertArrayEquals(password.toCharArray(), ((MongoClientImpl) mongoClient).getSettings().getCredential().getPassword());
    }
}
