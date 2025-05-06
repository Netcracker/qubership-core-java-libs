package org.qubership.cloud.dbaas.client.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.dbaas.client.entity.DatabaseCreateRequest;
import org.qubership.cloud.dbaas.client.entity.DbaasApiProperties;
import org.qubership.cloud.dbaas.client.entity.connection.PostgresDBConnection;
import org.qubership.cloud.dbaas.client.entity.database.PostgresDatabase;
import org.qubership.cloud.dbaas.client.entity.settings.PostgresSettings;
import org.qubership.cloud.dbaas.client.management.DatabasePool;
import org.qubership.cloud.dbaas.client.management.PostgresDatasourceCreator;
import org.qubership.cloud.framework.contexts.tenant.TenantContextObject;
import org.qubership.cloud.restclient.HttpMethod;
import org.qubership.cloud.restclient.MicroserviceRestClient;
import org.qubership.cloud.restclient.entity.RestClientResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.qubership.cloud.dbaas.client.config.DbaasPostgresConfiguration.SERVICE_POSTGRES_DATASOURCE;
import static org.qubership.cloud.dbaas.client.config.DbaasPostgresConfiguration.TENANT_POSTGRES_DATASOURCE;
import static org.qubership.cloud.framework.contexts.tenant.BaseTenantProvider.TENANT_CONTEXT_NAME;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {PostgresqlApiPropertiesTest.TestConfig.class},
        properties = {
                "dbaas.api.postgres.runtime-user-role=admin",
                "dbaas.api.postgres.db-prefix=test-prefix",
                "dbaas.api.postgres.service.database-settings.pg-extensions=pgSettingService1,pgSettingService2",
                "dbaas.api.postgres.tenant.database-settings.pg-extensions=pgSettingTenant3,pgSettingTenant4"
        })
public class PostgresqlApiPropertiesTest {

    @Autowired
    private MicroserviceRestClient microserviceRestClient;
    private static final String TENANT_ID = "test-tenant";

    @Autowired
    @Qualifier(SERVICE_POSTGRES_DATASOURCE)
    private DataSource serviceDataSource;
    @Autowired
    @Qualifier(TENANT_POSTGRES_DATASOURCE)
    private DataSource tenantDataSource;

    @SpyBean
    private PostgresDatasourceCreator datasourceCreator;

    @InjectMocks
    DatabasePool pool;
    @Autowired
    private DbaasApiProperties postgresDbaasApiProperties;

    @Test
    public void checkPostgresDbaasApiProperties(){
        Assertions.assertEquals("admin", postgresDbaasApiProperties.getRuntimeUserRole());
        Assertions.assertEquals("test-prefix", postgresDbaasApiProperties.getDbPrefix());
    }
    @Test
    public void checkPostgresqlServiceDatabaseApiProperties() throws SQLException {
        ArgumentCaptor<DatabaseCreateRequest> databaseCreateRequestArgumentCaptor = createRequest();
        serviceDataSource.getConnection();
        String actualRole = databaseCreateRequestArgumentCaptor.getValue().getUserRole();
        String prefix = databaseCreateRequestArgumentCaptor.getValue().getNamePrefix();
        PostgresSettings databaseSettings = (PostgresSettings) databaseCreateRequestArgumentCaptor.getValue().getSettings();
        Assertions.assertEquals("admin", actualRole);
        Assertions.assertEquals("test-prefix", prefix);
        Assertions.assertEquals("pgSettingService1", databaseSettings.getPgExtensions().get(0));
        Assertions.assertEquals("pgSettingService2", databaseSettings.getPgExtensions().get(1));

    }
    @Test
    public void checkPostgresqlTenantDatabaseApiProperties() throws SQLException {
        ContextManager.set(TENANT_CONTEXT_NAME, new TenantContextObject(TENANT_ID));
        ArgumentCaptor<DatabaseCreateRequest> databaseCreateRequestArgumentCaptor = createRequest();
        tenantDataSource.getConnection();
        String actualRole = databaseCreateRequestArgumentCaptor.getValue().getUserRole();
        String prefix = databaseCreateRequestArgumentCaptor.getValue().getNamePrefix();
        PostgresSettings databaseSettings = (PostgresSettings) databaseCreateRequestArgumentCaptor.getValue().getSettings();
        Assertions.assertEquals("admin", actualRole);
        Assertions.assertEquals("test-prefix", prefix);
        Assertions.assertEquals("pgSettingTenant3", databaseSettings.getPgExtensions().get(0));
        Assertions.assertEquals("pgSettingTenant4", databaseSettings.getPgExtensions().get(1));

    }
    private void mockDatasourceCreator(PostgresDatasourceCreator datasourceCreator) {
        Mockito.doAnswer(invocation -> {
            PostgresDatabase argument = (PostgresDatabase) invocation.getArgument(0);
            argument.getConnectionProperties().setDataSource(Mockito.mock(DataSource.class));
            return null;
        }).when(datasourceCreator).create(any(), any());
        Mockito.when(datasourceCreator.getSupportedDatabaseType()).thenCallRealMethod();
    }
    private ArgumentCaptor<DatabaseCreateRequest> createRequest(){
        ArgumentCaptor<DatabaseCreateRequest> databaseCreateRequestArgumentCaptor = ArgumentCaptor.forClass(DatabaseCreateRequest.class);
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
        Mockito.when(microserviceRestClient.doRequest(any(String.class), eq(HttpMethod.PUT), eq(null), databaseCreateRequestArgumentCaptor.capture(), eq(PostgresDatabase.class)))
                .thenReturn(restClientResponseEntity);
        return databaseCreateRequestArgumentCaptor;
    }

    @Configuration
    @EnableDbaasPostgresql
    public static class TestConfig {
        @Primary
        @Bean
        @Qualifier("dbaasRestClient")
        public static MicroserviceRestClient microserviceRestClient() {
            return Mockito.mock(MicroserviceRestClient.class);
        }
    }
}
