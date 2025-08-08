package org.qubership.cloud.dbaas.client.management;

import org.qubership.cloud.dbaas.client.entity.test.TestConnectorSettings;
import org.qubership.cloud.dbaas.client.entity.test.TestDBConnection;
import org.qubership.cloud.dbaas.client.entity.test.TestDatabase;
import org.qubership.cloud.dbaas.client.entity.test.TestDbClientCreator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;


public class DatabaseClientCreatorTest {

    @Test
    public void checkInteractionInCreateMethod(){
        DatabaseClientCreator<TestDatabase, TestConnectorSettings> databaseClientCreator = new TestDbClientCreator();
        TestDatabase database = new TestDatabase();
        TestDBConnection testDBConnection = Mockito.spy(TestDBConnection.class);
        database.setConnectionProperties(testDBConnection);

        databaseClientCreator.create(database);

        Assertions.assertNotNull(database.getConnectionProperties().getTestClient());
        Mockito.verify(testDBConnection).setTestClient(any());
    }
}
