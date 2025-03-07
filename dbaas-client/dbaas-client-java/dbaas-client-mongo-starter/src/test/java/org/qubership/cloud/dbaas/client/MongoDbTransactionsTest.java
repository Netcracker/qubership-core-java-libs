package org.qubership.cloud.dbaas.client;

import com.mongodb.ClientSessionOptions;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.client.ClientSession;
import org.qubership.cloud.dbaas.client.config.EnableDbaasMongo;
import org.qubership.cloud.dbaas.client.entity.connection.MongoDBConnection;
import org.qubership.cloud.dbaas.client.entity.database.MongoDatabase;
import org.qubership.cloud.dbaas.client.entity.database.type.MongoDBType;
import org.qubership.cloud.dbaas.client.management.DatabaseConfig;
import org.qubership.cloud.dbaas.client.management.DatabasePool;
import org.qubership.cloud.dbaas.client.management.DbaasDbClassifier;
import org.qubership.cloud.dbaas.client.management.DbaasMongoDbFactory;
import org.qubership.cloud.dbaas.client.management.MongoPostConnectProcessor;
import org.qubership.cloud.dbaas.client.test.Person;
import org.qubership.cloud.dbaas.client.test.configuration.MongoWithTransactionsTestContainerConfiguration;
import org.qubership.cloud.restclient.MicroserviceRestClient;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.qubership.cloud.dbaas.client.test.container.MongoTestContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.containers.MongoDBContainer;

import java.text.MessageFormat;

@SpringBootTest(classes = {
    MongoWithTransactionsTestContainerConfiguration.class,
    MongoDbTransactionsTest.TestConfig.class
})
@Slf4j
class MongoDbTransactionsTest {

    @Autowired
    @Qualifier("mongoWithTransactionsContainer")
    private MongoDBContainer container;

    @Autowired
    private DatabasePool databasePool;

    @Autowired
    private DbaasMongoDbFactory dbaasMongoDbFactory;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void setUp() {
        var defaultCodecRegistry = MongoClientSettings.getDefaultCodecRegistry();
        var spyClientSettingsBuilder = Mockito.spy(MongoClientSettings.builder());

        // intentionally don't set credential to client settings builder
        // to use mongodb container with created replica set and disabled security
        Mockito.doReturn(spyClientSettingsBuilder).when(spyClientSettingsBuilder)
            .credential(Mockito.any(MongoCredential.class));

        try(MockedStatic<MongoClientSettings> mockedStatic = Mockito.mockStatic(MongoClientSettings.class)) {
            mockedStatic.when(MongoClientSettings::getDefaultCodecRegistry).thenReturn(defaultCodecRegistry);
            mockedStatic.when(MongoClientSettings::builder).thenReturn(spyClientSettingsBuilder);

            var database = getMongoDatabase("test-db");
            new MongoPostConnectProcessor(null).process(database);

            Mockito.when(databasePool.getOrCreateDatabase(
                Mockito.eq(MongoDBType.INSTANCE), Mockito.any(DbaasDbClassifier.class)
            )).thenReturn(database);

            Mockito.when(databasePool.getOrCreateDatabase(
                Mockito.eq(MongoDBType.INSTANCE), Mockito.any(DbaasDbClassifier.class), Mockito.any(DatabaseConfig.class)
            )).thenReturn(database);
        }
    }

    @Test
    void testTransactionAbortedAndCollectionContainsOnlyOneDocument() {
        var sessionOptions = ClientSessionOptions.builder()
            .causallyConsistent(true)
            .build();

        var clientSession = dbaasMongoDbFactory.getSession(sessionOptions);
        var collectionName = "test-collection";
        var person = new Person("FirstName1", "LastName1");

        mongoTemplate.createCollection(collectionName);
        mongoTemplate.insert(person, collectionName);

        var transactionAbortedAfterInsertionDocument = Boolean.TRUE.equals(
            mongoTemplate.withSession(() -> clientSession).execute(action -> {
                var abortedAfterInsertionDocument = false;

                clientSession.startTransaction();
                log.info("Transaction has been started");

                try {
                    action.insert(new Person("FirstName2", "LastName2"), collectionName);
                    clientSession.abortTransaction();

                    abortedAfterInsertionDocument = true;
                    log.info("Transaction has been aborted");
                } catch (RuntimeException ex) {
                    log.error("Transaction has been aborted because of error: ", ex);
                }

                return abortedAfterInsertionDocument;
            }, ClientSession::close)
        );

        Assertions.assertTrue(transactionAbortedAfterInsertionDocument,
            "Transaction must be aborted after insertion second document in collection"
        );

        var foundPersons = mongoTemplate.findAll(Person.class, collectionName);

        Assertions.assertEquals(1, foundPersons.size(), MessageFormat.format(
            "Collection ''{0}'' must have only one saved document", collectionName
        ));

        var foundPerson = foundPersons.get(0);

        Assertions.assertEquals(person.toString(), foundPerson.toString(),
            MessageFormat.format("Found person ''{0}'' must me the same as previously saved person ''{1}''",
                foundPerson, person
            )
        );
    }

    private MongoDatabase getMongoDatabase(String dbName) {
        MongoDatabase database = new MongoDatabase();
        database.setName(dbName);
        MongoDBConnection connection = new MongoDBConnection();
        connection.setUrl("mongodb://" + container.getHost() + ":" + container.getMappedPort(MongoTestContainer.MONGO_PORT) + "/" + dbName);
        connection.setPassword(MongoTestContainer.MONGO_ADMIN_PWD);
        connection.setUsername(MongoTestContainer.MONGO_ADMIN_USERNAME);
        connection.setAuthDbName(MongoTestContainer.MONGO_ADMIN_DB);
        database.setConnectionProperties(connection);
        return database;
    }

    @Configuration
    @EnableDbaasMongo
    static class TestConfig {

        @Primary
        @Bean
        @Qualifier("dbaasRestClient")
        public static MicroserviceRestClient microserviceRestClient() {
            return Mockito.mock(MicroserviceRestClient.class);
        }

        @Primary
        @Bean
        public DatabasePool dbaasConnectionPoolMock() {
            return Mockito.mock(DatabasePool.class);
        }
    }
}
