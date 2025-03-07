package org.qubership.cloud.dbaas.client.management;

import org.qubership.cloud.dbaas.client.TestUtil;
import org.qubership.cloud.dbaas.client.config.EnableDbaasClient;
import org.qubership.cloud.dbaas.client.config.EnableDbaasPool;
import org.qubership.cloud.dbaas.client.entity.mongo.MongoDBConnection;
import org.qubership.cloud.dbaas.client.entity.mongo.MongoDBType;
import org.qubership.cloud.dbaas.client.entity.mongo.MongoDatabase;
import org.qubership.cloud.dbaas.client.entity.postgres.PostgresDBConnection;
import org.qubership.cloud.dbaas.client.entity.postgres.PostgresDBType;
import org.qubership.cloud.dbaas.client.entity.postgres.PostgresDatabase;
import org.qubership.cloud.restclient.HttpMethod;
import org.qubership.cloud.restclient.MicroserviceRestClient;
import org.qubership.cloud.restclient.entity.RestClientResponseEntity;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.util.UriTemplateHandler;

import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@EnableDbaasClient
@EnableDbaasPool
@ContextConfiguration(classes = {TestDbaasConfiguration.class})
public class DatabasePoolTest {

    public final static String DB_NAME = "dbName";
    public final static String DB_USER = "dbaas";
    public final static String AUTH_DB_NAME = "admin";
    public final static String DB_PASSWORD = "dbaas";
    public final static String MONGO_TEST_URI = "mongodb://"
            + DB_USER + ":" + DB_PASSWORD + "@localhost:27017,localhost:27018/";
    public final static String POSTGRES_TEST_URI = "jdbc:postgresql://localhost/";

    @Autowired
    private MicroserviceRestClient restClient;
    @Autowired
    private DatabasePool databasePool;
    @Autowired
    @Qualifier("testMongoPostProcessor")
    private PostConnectProcessor<MongoDatabase> mockMongoPostProcessor;
    @Autowired
    @Qualifier("testPostgresPostProcessor")
    private PostConnectProcessor<PostgresDatabase> mockPostgresPostProcessor;

    @BeforeEach
    public void init() {
        reset(restClient);
        reset(mockMongoPostProcessor);
        Mockito.when(mockMongoPostProcessor.getSupportedDatabaseType()).thenReturn(MongoDatabase.class);
        reset(mockPostgresPostProcessor);
        Mockito.when(mockPostgresPostProcessor.getSupportedDatabaseType()).thenReturn(PostgresDatabase.class);

        UriTemplateHandler uriTemplateHandler = Mockito.mock(UriTemplateHandler.class);

        MongoDatabase database = new MongoDatabase();
        database.setName(AUTH_DB_NAME);

        MongoDBConnection connection = new MongoDBConnection();
        connection.setUrl(MONGO_TEST_URI + AUTH_DB_NAME);
        connection.setUsername(DB_USER);
        connection.setAuthDbName(DB_NAME);
        connection.setPassword(DB_PASSWORD);
        database.setConnectionProperties(connection);

        when(restClient.doRequest(any(String.class), any(HttpMethod.class), isNull(), any(), eq(MongoDatabase.class)))
                .thenReturn(new RestClientResponseEntity<>(database, HttpStatus.OK.value()));


        PostgresDatabase postgresDatabase = new PostgresDatabase();
        postgresDatabase.setName(DB_NAME);

        PostgresDBConnection postgresDBConnection = new PostgresDBConnection();
        postgresDBConnection.setUrl(POSTGRES_TEST_URI + DB_NAME);
        postgresDBConnection.setUsername(DB_USER);
        postgresDBConnection.setPassword(DB_PASSWORD);
        postgresDatabase.setConnectionProperties(postgresDBConnection);

        when(restClient.doRequest(any(String.class), any(HttpMethod.class), isNull(), any(), eq(PostgresDatabase.class)))
                .thenReturn(new RestClientResponseEntity<>(postgresDatabase, HttpStatus.OK.value()));

    }

    @Test
    public void testPoolPostprocessorMongoInvoked() {
        databasePool.getOrCreateDatabase(MongoDBType.INSTANCE, getServiceClassifierBuilder());
        verify(mockMongoPostProcessor, times(1)).process(any(MongoDatabase.class));
        verify(mockPostgresPostProcessor, times(0)).process(any(PostgresDatabase.class));
    }

    @Test
    public void testPoolPostprocessorPostgresInvoked() {
        databasePool.getOrCreateDatabase(PostgresDBType.INSTANCE, getServiceClassifierBuilder());
        verify(mockMongoPostProcessor, times(0)).process(any(MongoDatabase.class));
        verify(mockPostgresPostProcessor, times(1)).process(any(PostgresDatabase.class));
    }


    private DbaasDbClassifier getServiceClassifierBuilder() {
        return new DbaasDbClassifier.Builder().withProperties(TestUtil.buildServiceClassifier("test", "test-ms")).build();
    }

    private DbaasDbClassifier getTenantClassifierBuilder(@Nullable String tenantId) {
        return new DbaasDbClassifier.Builder().withProperties(TestUtil.buildTenantClassifier("test", "test-ms", tenantId)).build();
    }
}
