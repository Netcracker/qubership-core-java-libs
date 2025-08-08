package com.netcracker.cloud.dbaas.client.entity.test;

import org.qubership.cloud.dbaas.client.entity.connection.DatabaseConnection;
import lombok.Getter;
import lombok.Setter;

public class TestDBConnection extends DatabaseConnection {
    @Getter
    private boolean closed = false;
    @Getter
    @Setter
    private TestClient testClient;


    public TestDBConnection(String url, String username, String password) {
        setUrl(url);
        setUsername(username);
        setPassword(password);
    }

    public TestDBConnection() {
    }

    public static class TestClient {
    }


    @Override
    public void close() throws Exception {
        closed = true;
    }
}
