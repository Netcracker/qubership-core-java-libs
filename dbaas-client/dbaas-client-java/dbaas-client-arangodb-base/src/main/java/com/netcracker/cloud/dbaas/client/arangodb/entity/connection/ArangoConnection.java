package com.netcracker.cloud.dbaas.client.arangodb.entity.connection;

import com.arangodb.ArangoDatabase;
import org.qubership.cloud.dbaas.client.entity.connection.DatabaseConnection;
import lombok.Data;

@Data
public class ArangoConnection extends DatabaseConnection {

    private String host;
    private int port;
    private String dbName;

    private ArangoDatabase arangoDatabase;

    @Override
    public void close() {
        if (arangoDatabase != null) {
            arangoDatabase.arango().shutdown();
        }
    }
}
