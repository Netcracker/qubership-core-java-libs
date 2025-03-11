package org.qubership.cloud.core.quarkus.dbaas.datasource;

import org.qubership.cloud.framework.contexts.tenant.TenantContextObject;
import org.qubership.cloud.framework.contexts.tenant.TenantProvider;
import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.core.quarkus.dbaas.datasource.classifier.TenantClassifierBuilder;
import org.qubership.cloud.core.quarkus.dbaas.datasource.service.DbaaSPostgresDbCreationService;
import org.qubership.cloud.dbaas.client.entity.connection.PostgresDBConnection;
import org.qubership.cloud.dbaas.client.entity.database.PostgresDatabase;
import io.agroal.api.AgroalDataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.qubership.cloud.framework.contexts.tenant.TenantProvider.TENANT_CONTEXT_NAME;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DbaasTenantDataSourceTest {
    private static DbaaSDataSource dbaasDataSource;
    private static DbaaSPostgresDbCreationService dataSourceCreationService = mock(DbaaSPostgresDbCreationService.class);

    @BeforeAll
    public static void initContext() {
        ContextManager.register(Collections.singletonList(new TenantProvider()));
    }

    @BeforeEach
    void prepare() {
        Map<String, Object> params = new HashMap<>();
        params.put("microserviceName", "test-service");
        params.put("tenantId", "test-tenant");
        dbaasDataSource = new DbaaSDataSource(new TenantClassifierBuilder(params), dataSourceCreationService);
        when(dataSourceCreationService.getOrCreatePostgresDatabase(any(), any(), any())).then((Answer<PostgresDatabase>) invocation -> {
            AgroalDataSource agroalDataSourceMock = mock(AgroalDataSource.class);
            PostgresDatabase postgresDbMock = mock(PostgresDatabase.class);
            PostgresDBConnection connectionMock = mock(PostgresDBConnection.class);
            when(agroalDataSourceMock.getConnection()).thenReturn(mock(Connection.class));
            when(postgresDbMock.getConnectionProperties()).thenReturn(connectionMock);
            when(connectionMock.getDataSource()).thenReturn(agroalDataSourceMock);
            return postgresDbMock;
        });

        ContextManager.set(TENANT_CONTEXT_NAME, new TenantContextObject("test-tenant"));
    }

    @Test
    public void mustReturnConnectionToDataSource() throws SQLException {
        Connection connection = dbaasDataSource.getConnection();
        assertNotNull(connection);
    }

    @Test
    public void mustCreateNewDataSourceForDifferentTenant() throws SQLException {
        Connection firstConnection = dbaasDataSource.getConnection();
        ContextManager.set(TENANT_CONTEXT_NAME, new TenantContextObject("other-test-tenant"));
        Connection secondConnection = dbaasDataSource.getConnection();

        assertNotEquals(firstConnection, secondConnection);
    }
}
