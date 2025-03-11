package org.qubership.cloud.core.quarkus.dbaas.datasource;

import org.qubership.cloud.core.quarkus.dbaas.datasource.classifier.MicroserviceClassifierBuilder;
import org.qubership.cloud.core.quarkus.dbaas.datasource.service.DbaaSPostgresDbCreationService;
import org.qubership.cloud.dbaas.client.entity.connection.PostgresDBConnection;
import org.qubership.cloud.dbaas.client.entity.database.PostgresDatabase;
import org.qubership.cloud.dbaas.client.management.DatabaseConfig;
import io.agroal.api.AgroalDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DbaasServiceDataSourceTest {
    private static DbaaSDataSource dbaasDataSource;
    private static DbaaSPostgresDbCreationService dataSourceCreationService = mock(DbaaSPostgresDbCreationService.class);

    @BeforeEach
    void prepare() {
        Map<String, Object> params = new HashMap<>();
        params.put("microserviceName", "test-service");
        params.put("scope", "service");
        DatabaseConfig databaseConfig = DatabaseConfig.builder()
                .backupDisabled(true)
                .build();
        dbaasDataSource = new DbaaSDataSource(new MicroserviceClassifierBuilder(params), dataSourceCreationService, databaseConfig);
        when(dataSourceCreationService.getOrCreatePostgresDatabase(any(), any(), any())).then((Answer<PostgresDatabase>) invocation -> {
            AgroalDataSource agroalDataSourceMock = mock(AgroalDataSource.class);
            PostgresDatabase postgresDbMock = mock(PostgresDatabase.class);
            PostgresDBConnection connectionMock = mock(PostgresDBConnection.class);
            when(agroalDataSourceMock.getConnection()).thenReturn(mock(Connection.class));
            when(postgresDbMock.getConnectionProperties()).thenReturn(connectionMock);
            when(connectionMock.getDataSource()).thenReturn(agroalDataSourceMock);
            return postgresDbMock;
        });
    }

    @Test
    public void mustReturnConnectionToDataSource() throws SQLException {
        Connection connection = dbaasDataSource.getConnection();
        assertNotNull(connection);
    }

    @Test
    void testGetInnerDatasource() {
        assertInstanceOf(AgroalDataSource.class, dbaasDataSource.getInnerDataSource());
    }
}