package org.qubership.cloud.quarkus.dbaas.mongoclient;

import org.qubership.cloud.quarkus.dbaas.mongoclient.entity.connection.MongoDBConnection;

public class CommonMongoTestPart {

    public static MongoDBConnection prepareMongoDbConnection() {
        MongoDBConnection mongoDBConnection = new MongoDBConnection();
        mongoDBConnection.setUrl("test-url");
        mongoDBConnection.setUsername("test-username");
        mongoDBConnection.setPassword("test-password");
        mongoDBConnection.setUrl("mongodb://admin");
        mongoDBConnection.setAuthDbName("dbName");
        return mongoDBConnection;
    }

}
