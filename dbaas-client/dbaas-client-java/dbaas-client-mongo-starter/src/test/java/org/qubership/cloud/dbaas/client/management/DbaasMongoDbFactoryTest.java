package org.qubership.cloud.dbaas.client.management;

import com.mongodb.ClientSessionOptions;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import org.qubership.cloud.dbaas.client.entity.connection.MongoDBConnection;
import org.qubership.cloud.dbaas.client.entity.database.MongoDatabase;
import org.qubership.cloud.dbaas.client.entity.database.type.MongoDBType;
import org.qubership.cloud.dbaas.client.management.classifier.DbaaSClassifierBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.support.PersistenceExceptionTranslator;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DbaasMongoDbFactoryTest extends AbstractMongoDbFactoryTest {

    @Test
    void testGetSession() {
        DbaasDbClassifier dbaasDbClassifier = defaultDbaasDbClassifier().build();
        when(databasePool.getOrCreateDatabase(eq(MongoDBType.INSTANCE), eq(dbaasDbClassifier))).thenReturn(mongoDatabase);
        when(mongoDatabase.getConnectionProperties()).thenReturn(mongoDBConnection);
        when(mongoDBConnection.getClient()).thenReturn(mongoClient);

        ClientSessionOptions clientSessionOptions = ClientSessionOptions.builder().build();
        dbaasMongoDbFactory.getSession(clientSessionOptions);

        verify(mongoClient, times(1)).startSession(clientSessionOptions);
    }

    @Test
    void testWithSession() {
        var clientSession = mock(ClientSession.class);
        var dbaasMongoDbFactoryWithSession = dbaasMongoDbFactory.withSession(clientSession);

        Assertions.assertEquals(ClientSessionBoundMongoDbFactory.class,
            dbaasMongoDbFactoryWithSession.getClass()
        );

        var clientSessionBoundMongoDbFactory = (ClientSessionBoundMongoDbFactory) dbaasMongoDbFactoryWithSession;

        Assertions.assertEquals(dbaasMongoDbFactory, clientSessionBoundMongoDbFactory.getDelegate());
        Assertions.assertEquals(clientSession, clientSessionBoundMongoDbFactory.getSession());
    }
}