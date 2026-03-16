package com.netcracker.cloud.dbaas.client;

import com.netcracker.cloud.dbaas.client.config.DbaasMongoConfiguration;
import com.netcracker.cloud.dbaas.client.entity.DatabaseCreateRequest;
import com.netcracker.cloud.dbaas.client.entity.connection.MongoDBConnection;
import com.netcracker.cloud.dbaas.client.entity.database.MongoDatabase;
import com.netcracker.cloud.dbaas.client.entity.database.type.MongoDBType;
import com.netcracker.cloud.dbaas.client.management.DatabaseDefinitionHandler;
import com.netcracker.cloud.dbaas.client.management.DatabasePool;
import com.netcracker.cloud.dbaas.client.management.DbaasDbClassifier;
import com.netcracker.cloud.dbaas.client.management.PostConnectProcessor;
import com.netcracker.cloud.dbaas.client.test.configuration.CreateDatabaseConfig;
import com.netcracker.cloud.restclient.HttpMethod;
import com.netcracker.cloud.restclient.MicroserviceRestClient;
import com.netcracker.cloud.restclient.entity.RestClientResponseEntity;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;

import java.util.Arrays;

import static com.netcracker.cloud.dbaas.client.DbaasConst.*;
import static com.netcracker.cloud.dbaas.client.test.TestConstants.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Slf4j
@SpringBootTest
@ContextConfiguration(classes = {CreateDatabaseConfig.class, DbaasMongoConfiguration.class})
public class CreateDatabaseTest {
    private DatabasePool databasePool;

    @Autowired
    private DbaasClient dbaasClient;

    @Autowired
    @Qualifier("testMongoPostProcessor")
    private PostConnectProcessor<MongoDatabase> mockMongoPostProcessor;

    @Autowired
    @Qualifier("mongoPostConnectProcessor")
    private PostConnectProcessor<MongoDatabase> mongoPostConnectProcessor;

    @Autowired
    private MicroserviceRestClient restClient;

    private MongoDBConnection connection;

    @BeforeEach
    public void setup() {
        MongoDatabase database = new MongoDatabase();
        database.setName(AUTH_DB_NAME);

        connection = Mockito.spy(MongoDBConnection.class);
        connection.setUrl(MONGO_TEST_URI + AUTH_DB_NAME);
        connection.setUsername(DB_USER);
        connection.setAuthDbName(DB_NAME);
        connection.setPassword(DB_PASSWORD);
        database.setConnectionProperties(connection);

        Mockito.when(mockMongoPostProcessor.getSupportedDatabaseType()).thenReturn(MongoDatabase.class);
        Mockito.doThrow(new RuntimeException("test-exception")).when(mockMongoPostProcessor).process(database);

        databasePool = new DatabasePool(
                dbaasClient,
                "test-ms",
                "test-space",
                Arrays.asList(mongoPostConnectProcessor, mockMongoPostProcessor),
                Mockito.mock(DatabaseDefinitionHandler.class),
                AnnotationAwareOrderComparator.INSTANCE,
                null, null);
        when(restClient.doRequest(any(String.class), eq(HttpMethod.PUT), isNull(), any(DatabaseCreateRequest.class), eq(MongoDatabase.class)))
                .thenReturn(new RestClientResponseEntity<>(database, HttpStatus.OK.value(), null));
    }

    @Test
    public void testCloseMongodbConnectionAfterException() throws Exception {
        int createDbCount = 100;
        for (int i = 0; i < createDbCount; i++) {
            try {
                databasePool.getOrCreateDatabase(MongoDBType.INSTANCE, new DbaasDbClassifier.Builder()
                        .withProperty(SCOPE, SERVICE)
                        .withProperty(NAMESPACE, "test-namespace")
                        .withProperty(MICROSERVICE_NAME, "test")
                        .build());
            } catch (Throwable throwable) {
                log.info("{} call getOrCreateDatabase method", i);
            }
        }
        verify(connection, times(100)).close();
    }
}
