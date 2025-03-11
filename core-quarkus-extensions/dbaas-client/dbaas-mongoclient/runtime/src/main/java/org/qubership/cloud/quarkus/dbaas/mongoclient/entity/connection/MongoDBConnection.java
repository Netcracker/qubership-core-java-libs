package org.qubership.cloud.quarkus.dbaas.mongoclient.entity.connection;

import com.mongodb.client.MongoClient;
import org.qubership.cloud.dbaas.client.entity.connection.DatabaseConnection;
import lombok.Data;
import lombok.ToString;

@Data
@ToString(of = {"authDbName", "dbName"}) // do not initiate client by invoking getClient() on toString() invocation
public class MongoDBConnection extends DatabaseConnection {
    private String authDbName;
    private String dbName;

    private volatile MongoClient client;

    @Override
    public void close() throws Exception {
        if (client != null) {
            client.close();
        }
    }
}
