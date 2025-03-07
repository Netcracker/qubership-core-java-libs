package org.qubership.cloud.dbaas.client.redis.entity.connection;

import org.qubership.cloud.dbaas.client.entity.connection.DatabaseConnection;
import lombok.Data;

@Data
public abstract class AbstractRedisDBConnection<T> extends DatabaseConnection {

    private String host;
    private int port;
    private String service;

    private T connection;

    @Override
    public void close() throws Exception {
        if (connection instanceof AutoCloseable autoCloseable) {
            autoCloseable.close();
        }
    }
}
