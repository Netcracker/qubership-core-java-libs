package com.netcracker.cloud.dbaas.client.entity.database;

import com.netcracker.cloud.dbaas.client.entity.test.TestDBConnection;
import com.netcracker.cloud.dbaas.client.entity.test.TestDatabase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AbstractDatabaseTest {

    @Test
    public void testCloseConnection() throws Exception {
        final TestDatabase testDatabase = new TestDatabase();
        final TestDBConnection testConnection = new TestDBConnection();
        testDatabase.setConnectionProperties(testConnection);

        assertFalse(testConnection.isClosed());

        testDatabase.setDoClose(false);
        testDatabase.close();
        assertFalse(testConnection.isClosed());

        testDatabase.setDoClose(true);
        testDatabase.close();
        assertTrue(testConnection.isClosed());
    }

    @Test
    public void testToStringWithMaskedPassword() throws Exception {
        final TestDatabase testDatabase = new TestDatabase();

        String expectedURL = "mongodb://root:***@mongos.mongodb-main.svc:27017/eso5-service-ordering?authSource=admin";
        final TestDBConnection testConnection = new TestDBConnection("mongodb://root:root@mongos.mongodb-main.svc:27017/eso5-service-ordering?authSource=admin", "username", "password");
        testDatabase.setConnectionProperties(testConnection);

        assertTrue(testDatabase.toString().contains(expectedURL));

        String expectedURL2 = "mongodb://mongos.mongodb-main.svc:27017/eso5-service-ordering?authSource=admin";
        final TestDBConnection testConnection2 = new TestDBConnection(expectedURL2, "username", "password");
        testDatabase.setConnectionProperties(testConnection2);

        assertTrue(testDatabase.toString().contains(expectedURL2));
        testDatabase.close();
    }
}
