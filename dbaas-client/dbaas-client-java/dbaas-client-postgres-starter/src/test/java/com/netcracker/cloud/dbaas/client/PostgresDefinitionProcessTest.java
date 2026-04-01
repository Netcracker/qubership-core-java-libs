package com.netcracker.cloud.dbaas.client;

import com.netcracker.cloud.dbaas.client.config.EnableDbaasPostgresql;
import com.netcracker.cloud.dbaas.client.entity.DatabaseCreateRequest;
import com.netcracker.cloud.dbaas.client.entity.connection.PostgresDBConnection;
import com.netcracker.cloud.dbaas.client.entity.database.PostgresDatabase;
import com.netcracker.cloud.dbaas.client.management.AbstractPostgresDefinitionProcess;
import com.netcracker.cloud.dbaas.client.management.PostgresDatasourceCreator;
import com.netcracker.cloud.restclient.HttpMethod;
import com.netcracker.cloud.restclient.MicroserviceRestClient;
import com.netcracker.cloud.restclient.entity.RestClientResponseEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static com.netcracker.cloud.dbaas.client.config.DbaasPostgresConfiguration.*;

@SpringBootTest(classes = {PostgresDefinitionProcessTest.TestConfig.class},
        properties = {
                "dbaas.api.postgres.runtime-user-role=rw",
                "spring.datasource.hikari.initializationFailTimeout=-1"
        })
public class PostgresDefinitionProcessTest {

    @Autowired
    @Qualifier(SERVICE_POSTGRES_DATASOURCE)
    private DataSource serviceDataSource;

    @Autowired
    private MicroserviceRestClient microserviceRestClient;

    @Autowired
    private PgDefinitionProcess pgDefinitionProcess;

    @MockitoSpyBean
    private PostgresDatasourceCreator datasourceCreator;

    @Test
    public void checkPostgresDefinitionProcessIsCalled() throws SQLException {
        Map<String, Object> props = new HashMap<>();
        props.put("initializationFailTimeout", -1);
        mockDatasourceCreator(datasourceCreator);
        Mockito.reset(microserviceRestClient);
        RestClientResponseEntity restClientResponseEntity = Mockito.mock(RestClientResponseEntity.class);
        PostgresDatabase postgresDatabase = new PostgresDatabase();
        PostgresDBConnection postgresDBConnection = new PostgresDBConnection();
        postgresDBConnection.setUsername("username");
        postgresDBConnection.setPassword("password");
        postgresDBConnection.setUrl("jdbc:postgresql://localhost:5432/test_db");
        postgresDatabase.setConnectionProperties(postgresDBConnection);
        Mockito.when(restClientResponseEntity.getResponseBody()).thenReturn(postgresDatabase);
        Mockito.when(restClientResponseEntity.getHttpStatus()).thenReturn(200);
        Mockito.when(microserviceRestClient.doRequest(any(String.class), eq(HttpMethod.PUT), eq(null),
                        any(DatabaseCreateRequest.class), eq(PostgresDatabase.class)))
                .then(invocation -> {
                    String userRole = invocation.getArgument(3, DatabaseCreateRequest.class).getUserRole();
                    Assertions.assertEquals("admin", userRole);
                    return restClientResponseEntity;
                })
                .then(invocation -> {
                    String userRole = invocation.getArgument(3, DatabaseCreateRequest.class).getUserRole();
                    Assertions.assertEquals("rw", userRole);
                    return restClientResponseEntity;
                });

        serviceDataSource.getConnection();

        Assertions.assertTrue(pgDefinitionProcess.isCalled());
        Mockito.verify(microserviceRestClient, Mockito.times(2))
                .doRequest(any(String.class), eq(HttpMethod.PUT), eq(null),
                        any(DatabaseCreateRequest.class), eq(PostgresDatabase.class));
    }

    @Test
    public void checkPgSupportedType() {
        Assertions.assertEquals(PostgresDatabase.class, pgDefinitionProcess.getSupportedDatabaseType());
    }

    private void mockDatasourceCreator(PostgresDatasourceCreator datasourceCreator) {
        Mockito.doAnswer(invocation -> {
            PostgresDatabase argument = (PostgresDatabase) invocation.getArgument(0);
            argument.getConnectionProperties().setDataSource(Mockito.mock(DataSource.class));
            return null;
        }).when(datasourceCreator).create(any(), any());
        Mockito.when(datasourceCreator.getSupportedDatabaseType()).thenCallRealMethod();
    }


    @EnableDbaasPostgresql
    public static class TestConfig {
        @Bean
        public PgDefinitionProcess pgDefinitionProcess() {
            return new PgDefinitionProcess();
        }

        @Primary
        @Bean
        @Qualifier("dbaasRestClient")
        public static MicroserviceRestClient microserviceRestClient() {
            return Mockito.mock(MicroserviceRestClient.class);
        }
    }

    public static class PgDefinitionProcess extends AbstractPostgresDefinitionProcess {

        private Boolean isCalled = false;

        @Override
        public void process(PostgresDatabase database) {
            isCalled = true;
        }

        public Boolean isCalled() {
            return isCalled;
        }
    }
}



