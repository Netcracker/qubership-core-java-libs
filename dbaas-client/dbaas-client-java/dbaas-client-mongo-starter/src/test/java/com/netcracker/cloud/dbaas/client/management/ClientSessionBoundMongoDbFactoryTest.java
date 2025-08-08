package com.netcracker.cloud.dbaas.client.management;

import com.mongodb.ClientSessionOptions;
import com.mongodb.client.ClientSession;
import org.qubership.cloud.dbaas.client.entity.database.type.MongoDBType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientSessionBoundMongoDbFactoryTest extends AbstractMongoDbFactoryTest {

    @Test
    void testGetMongoDatabase() {
        var dbaasDbClassifier = defaultDbaasDbClassifier().build();
        var spyClientMongoDatabase = spy(com.mongodb.client.MongoDatabase.class);

        when(databasePool.getOrCreateDatabase(eq(MongoDBType.INSTANCE), eq(dbaasDbClassifier), eq(databaseConfig))).thenReturn(mongoDatabase);
        when(mongoDatabase.getConnectionProperties()).thenReturn(mongoDBConnection);
        when(mongoDBConnection.getClient()).thenReturn(mongoClient);
        when(mongoClient.getDatabase(any())).thenReturn(spyClientMongoDatabase);

        var spyDelegateDbaasMongoDbFactory = spy(new DbaasMongoDbFactory(dbaasClassifierBuilder, databasePool, databaseConfig));
        var clientSession = mock(ClientSession.class);
        var dbaasMongoDbFactoryWithSession = spyDelegateDbaasMongoDbFactory.withSession(clientSession);
        var dbName = "test-database";

        dbaasMongoDbFactoryWithSession.getMongoDatabase();
        dbaasMongoDbFactoryWithSession.getMongoDatabase(dbName);

        verify(spyDelegateDbaasMongoDbFactory, times(1)).getMongoDatabase();
        verify(spyDelegateDbaasMongoDbFactory, times(1)).getMongoDatabase(eq(dbName));
    }

    @Test
    void testGetExceptionTranslator() {
        var spyDelegateDbaasMongoDbFactory = spy(new DbaasMongoDbFactory(dbaasClassifierBuilder, databasePool, databaseConfig));
        var clientSession = mock(ClientSession.class);
        var dbaasMongoDbFactoryWithSession = spyDelegateDbaasMongoDbFactory.withSession(clientSession);

        dbaasMongoDbFactoryWithSession.getExceptionTranslator();

        verify(spyDelegateDbaasMongoDbFactory, times(1)).getExceptionTranslator();
    }

    @Test
    void testGetSession() {
        var dbaasDbClassifier = defaultDbaasDbClassifier().build();
        var options = mock(ClientSessionOptions.class);
        var clientSession = mock(ClientSession.class);

        when(databasePool.getOrCreateDatabase(eq(MongoDBType.INSTANCE), eq(dbaasDbClassifier))).thenReturn(mongoDatabase);
        when(mongoDatabase.getConnectionProperties()).thenReturn(mongoDBConnection);
        when(mongoDBConnection.getClient()).thenReturn(mongoClient);
        when(mongoClient.startSession(eq(options))).thenReturn(clientSession);

        var spyDelegateDbaasMongoDbFactory = spy(new DbaasMongoDbFactory(dbaasClassifierBuilder, databasePool, databaseConfig));
        var dbaasMongoDbFactoryWithSession = spyDelegateDbaasMongoDbFactory.withSession(clientSession);

        dbaasMongoDbFactoryWithSession.getSession(options);

        verify(spyDelegateDbaasMongoDbFactory, times(1)).getSession(eq(options));
    }

    @Test
    void testWithSession() {
        var spyDelegateDbaasMongoDbFactory = spy(new DbaasMongoDbFactory(dbaasClassifierBuilder, databasePool, databaseConfig));
        var clientSession = mock(ClientSession.class);
        var dbaasMongoDbFactoryWithSession = spyDelegateDbaasMongoDbFactory.withSession(clientSession);

        reset(spyDelegateDbaasMongoDbFactory);

        dbaasMongoDbFactoryWithSession.withSession(clientSession);

        verify(spyDelegateDbaasMongoDbFactory, times(1)).withSession(eq(clientSession));
    }

    @Test
    void testIsTransactionActive() {
        var dbaasMongoDbFactory = new DbaasMongoDbFactory(dbaasClassifierBuilder, databasePool, databaseConfig);
        var clientSession = mock(ClientSession.class);
        var dbaasMongoDbFactoryWithSession = dbaasMongoDbFactory.withSession(clientSession);

        Assertions.assertFalse(dbaasMongoDbFactoryWithSession.isTransactionActive());
    }

    @Test
    void testMongoDatabaseCreatesCollectionWithImplicitlyPassedClientSessionArgument() {
        var dbaasDbClassifier = defaultDbaasDbClassifier().build();
        var spyClientMongoDatabase = spy(com.mongodb.client.MongoDatabase.class);

        when(databasePool.getOrCreateDatabase(eq(MongoDBType.INSTANCE), eq(dbaasDbClassifier), eq(databaseConfig))).thenReturn(mongoDatabase);
        when(mongoDatabase.getConnectionProperties()).thenReturn(mongoDBConnection);
        when(mongoDBConnection.getClient()).thenReturn(mongoClient);
        when(mongoClient.getDatabase(any())).thenReturn(spyClientMongoDatabase);

        var clientSession = mock(ClientSession.class);
        var dbaasMongoDbFactoryWithSession = dbaasMongoDbFactory.withSession(clientSession);
        var clientMongoDatabase = dbaasMongoDbFactoryWithSession.getMongoDatabase();
        var collectionName = "test-collection";

        clientMongoDatabase.createCollection(collectionName);

        verify(spyClientMongoDatabase, times(0))
            .createCollection(eq(collectionName));

        verify(spyClientMongoDatabase, times(1))
            .createCollection(eq(clientSession), eq(collectionName));
    }
}
