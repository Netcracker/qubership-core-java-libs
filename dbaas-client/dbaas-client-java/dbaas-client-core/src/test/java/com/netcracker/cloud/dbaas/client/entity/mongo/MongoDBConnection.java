package com.netcracker.cloud.dbaas.client.entity.mongo;

import org.qubership.cloud.dbaas.client.entity.connection.DatabaseConnection;
import lombok.Data;
import lombok.ToString;

@Data
@ToString(of = {"authDbName"}) // do not initiate client by invoking getClient() on toString() invocation
public class MongoDBConnection extends DatabaseConnection {
    private String authDbName;


    @Override
    public void close() {}
}
