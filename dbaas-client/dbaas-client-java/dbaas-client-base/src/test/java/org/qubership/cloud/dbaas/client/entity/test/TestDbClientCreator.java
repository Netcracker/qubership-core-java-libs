package org.qubership.cloud.dbaas.client.entity.test;

import org.qubership.cloud.dbaas.client.management.DatabaseClientCreator;

public class TestDbClientCreator implements DatabaseClientCreator<TestDatabase, TestConnectorSettings> {

    @Override
    public void create(TestDatabase database) {
        database.getConnectionProperties().setTestClient(new TestDBConnection.TestClient());
    }

    @Override
    public void create(TestDatabase database,  TestConnectorSettings settings) {
        database.getConnectionProperties().setTestClient(new TestDBConnection.TestClient());
    }

    @Override
    public Class<TestDatabase> getSupportedDatabaseType() {
        return TestDatabase.class;
    }
}
