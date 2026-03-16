package com.netcracker.cloud.dbaas.client.entity.connection;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;


public class DatabaseConnectionTest {

    @Test
    public void testToStringDoesntPrintPassword() {
        final String password = UUID.randomUUID().toString();

        DatabaseConnection conn = new DatabaseConnection() {
            @Override
            public void close() {}
        };
        conn.setUrl("test-url");
        conn.setUsername("test-user");
        conn.setPassword(password);

        assertFalse(conn.toString().contains(password));
    }
}
