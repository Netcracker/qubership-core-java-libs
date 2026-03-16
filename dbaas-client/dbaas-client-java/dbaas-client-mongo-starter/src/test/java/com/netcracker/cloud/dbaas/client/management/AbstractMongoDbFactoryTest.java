package com.netcracker.cloud.dbaas.client.management;

import com.mongodb.client.MongoClient;
import com.netcracker.cloud.dbaas.client.entity.connection.MongoDBConnection;
import com.netcracker.cloud.dbaas.client.entity.database.MongoDatabase;
import com.netcracker.cloud.dbaas.client.management.classifier.DbaaSClassifierBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.dao.support.PersistenceExceptionTranslator;

import static org.mockito.Mockito.reset;

abstract class AbstractMongoDbFactoryTest {

    protected final DbaaSClassifierBuilder dbaasClassifierBuilder = defaultDbaasDbClassifier();

    @Mock
    protected DatabasePool databasePool;
    @Mock
    protected PersistenceExceptionTranslator persistenceExceptionTranslator;
    @Mock
    protected MongoDatabase mongoDatabase;
    @Mock
    protected MongoDBConnection mongoDBConnection;
    @Mock
    protected MongoClient mongoClient;
    @Mock
    protected DatabaseConfig databaseConfig;

    @InjectMocks
    protected DbaasMongoDbFactory dbaasMongoDbFactory = new DbaasMongoDbFactory(dbaasClassifierBuilder, databasePool, persistenceExceptionTranslator, databaseConfig);

    @BeforeEach
    protected void setUp() {
        reset(databasePool, persistenceExceptionTranslator);
    }

    protected DbaaSClassifierBuilder defaultDbaasDbClassifier() {
        return new DbaasDbClassifier.Builder()
            .withProperty("dbClassifier", "default")
            .withProperty("isServiceDb", true);
    }
}
