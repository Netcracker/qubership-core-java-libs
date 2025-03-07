package org.qubership.cloud.dbaas.client.arangodb.test;

import org.qubership.cloud.dbaas.client.arangodb.entity.connection.ArangoConnection;
import org.qubership.cloud.dbaas.client.arangodb.entity.database.ArangoDatabase;

public class ArangoTestCommon {
    public static final String DB_NAME = "db-test-name";
    public static final String DB_HOST = "test-arangodb-host";
    public static final String TEST_USER = "test-username";
    public static final String TEST_PASSWORD = "test-password";
    public static final int DB_PORT = 8529;

    public static ArangoDatabase createArangoDatabase(String dbName, String dbHost, int dbPort) {
        ArangoDatabase database = new ArangoDatabase();
        database.setName(dbName);
        ArangoConnection arangoConnection = new ArangoConnection();
        arangoConnection.setHost(dbHost);
        arangoConnection.setPort(dbPort);
        arangoConnection.setDbName(dbName);
        database.setConnectionProperties(arangoConnection);
        return database;
    }

}
