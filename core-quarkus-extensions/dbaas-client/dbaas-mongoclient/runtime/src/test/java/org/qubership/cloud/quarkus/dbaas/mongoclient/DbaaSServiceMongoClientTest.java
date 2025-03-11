package org.qubership.cloud.quarkus.dbaas.mongoclient;

import com.mongodb.client.MongoClient;
import org.qubership.cloud.dbaas.client.management.DbaasDbClassifier;
import org.qubership.cloud.quarkus.dbaas.mongoclient.classifier.ServiceClassifierBuilder;
import org.qubership.cloud.quarkus.dbaas.mongoclient.entity.connection.MongoDBConnection;
import org.qubership.cloud.quarkus.dbaas.mongoclient.entity.database.MongoDatabase;
import org.qubership.cloud.quarkus.dbaas.mongoclient.service.MongoClientCreation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.qubership.cloud.dbaas.client.DbaasConst.SCOPE;
import static org.qubership.cloud.dbaas.client.DbaasConst.SERVICE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DbaaSServiceMongoClientTest {

    private static DbaaSMongoClient dbaaSMongoClient;
    private static final MongoClientCreation mongoClientCreation = mock(MongoClientCreation.class);
    private static final MongoClient client = mock(MongoClient.class);
    private static MongoDatabase mongoDatabase;

    @BeforeEach
    void prepare() {
        Map<String, Object> params = new HashMap<>();
        params.put("microserviceName", "test-service");
        params.put("dbClassifier", "default");
        params.put(SCOPE, SERVICE);
        dbaaSMongoClient = new DbaaSMongoClient(new ServiceClassifierBuilder(params), mongoClientCreation);
        MongoDBConnection mongoDBConnection = CommonMongoTestPart.prepareMongoDbConnection();
        mongoDBConnection.setClient(client);

        mongoDatabase = new MongoDatabase();
        mongoDatabase.setConnectionProperties(mongoDBConnection);
    }

    @Test
    public void mustReturnSameServiceMongoDatabase() {
        Map<String, Object> params = new HashMap<>();
        params.put("microserviceName", "test-service");
        params.put("dbClassifier", "default");
        params.put(SCOPE, SERVICE);
        DbaasDbClassifier classifier = new DbaasDbClassifier(params);
        SortedMap<String, Object> newMap = new TreeMap<>(classifier.asMap());

        mongoDatabase.setClassifier(newMap);
        when(mongoClientCreation.getOrCreateMongoDatabase(any())).thenReturn(mongoDatabase);
        MongoClient firstDb = dbaaSMongoClient.getOrCreateMongoDb().getConnectionProperties().getClient();
        assertNotNull(firstDb);
        MongoClient secondDb = dbaaSMongoClient.getOrCreateMongoDb().getConnectionProperties().getClient();
        assertEquals(firstDb, secondDb);
    }

}