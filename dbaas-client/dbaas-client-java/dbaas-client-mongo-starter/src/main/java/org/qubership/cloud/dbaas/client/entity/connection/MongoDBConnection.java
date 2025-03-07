package org.qubership.cloud.dbaas.client.entity.connection;

import com.mongodb.client.MongoClient;
import lombok.Data;
import lombok.ToString;

@Data
@ToString(callSuper = true, of = {"authDbName"})
// do not initiate client by invoking getClient() on toString() invocation
public class MongoDBConnection extends DatabaseConnection {
    private String authDbName;

    private volatile MongoClient client;

    @Override
    public void close() throws Exception {
        if (client != null) {
            client.close();
        }
    }
}
