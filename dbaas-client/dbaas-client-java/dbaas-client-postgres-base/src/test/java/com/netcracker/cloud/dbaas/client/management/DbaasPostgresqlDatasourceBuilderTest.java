package com.netcracker.cloud.dbaas.client.management;

import org.qubership.cloud.dbaas.client.entity.database.DatasourceConnectorSettings;
import org.qubership.cloud.dbaas.client.entity.database.type.PostgresDBType;
import org.qubership.cloud.dbaas.client.management.classifier.ServiceDbaaSClassifierBuilder;
import org.qubership.cloud.dbaas.client.service.flyway.FlywayRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class DbaasPostgresqlDatasourceBuilderTest {

    @Mock
    private DatabasePool databasePool;

    @InjectMocks
    private DbaasPostgresqlDatasourceBuilder datasourceBuilder;
    @Captor
    private ArgumentCaptor<DatasourceConnectorSettings> connectorSettingsCapture;

    @Captor
    private ArgumentCaptor<DatabaseConfig> databaseConfigCapture;

    @Test
    public void testWithSchema() {
        DataSource dataSource = datasourceBuilder.newBuilder(new ServiceDbaaSClassifierBuilder())
                .withSchema("test_schema")
                .build();

        ((DbaasPostgresProxyDataSource) dataSource).getDatabase();

        Mockito.verify(databasePool).getOrCreateDatabase(eq(PostgresDBType.INSTANCE), any(DbaasDbClassifier.class), any(), connectorSettingsCapture.capture());
        assertEquals("test_schema", connectorSettingsCapture.getValue().getSchema());
    }

    @Test
    public void testWithDatabaseConfig() {
        DatabaseConfig databaseConfig = DatabaseConfig.builder()
                .userRole("testUser")
                .build();

        DataSource dataSource = datasourceBuilder.newBuilder(new ServiceDbaaSClassifierBuilder())
                .withDatabaseConfig(databaseConfig)
                .build();

        ((DbaasPostgresProxyDataSource) dataSource).getDatabase();

        Mockito.verify(databasePool).getOrCreateDatabase(eq(PostgresDBType.INSTANCE), any(DbaasDbClassifier.class), databaseConfigCapture.capture(), connectorSettingsCapture.capture());
        assertEquals("testUser", databaseConfigCapture.getValue().getUserRole());
    }

    @Test
    public void testWithConnectionProperties() {
        Map<String, Object> connectionProperties = new HashMap<>();
        connectionProperties.put("property1", "value1");
        connectionProperties.put("property2", "value2");

        DataSource dataSource = datasourceBuilder.newBuilder(new ServiceDbaaSClassifierBuilder())
                .withConnectionProperties(connectionProperties)
                .build();

        ((DbaasPostgresProxyDataSource) dataSource).getDatabase();

        Mockito.verify(databasePool).getOrCreateDatabase(eq(PostgresDBType.INSTANCE), any(DbaasDbClassifier.class), any(), connectorSettingsCapture.capture());
        assertEquals("value1", connectorSettingsCapture.getValue().getConnPropertiesParam().get("property1"));
        assertEquals("value2", connectorSettingsCapture.getValue().getConnPropertiesParam().get("property2"));
    }

    @Test
    public void testWithFlywayRunner() {
        FlywayRunner flywayRunner = mock(FlywayRunner.class);

        DataSource dataSource = datasourceBuilder.newBuilder(new ServiceDbaaSClassifierBuilder())
                .withFlyway(flywayRunner)
                .build();
        ((DbaasPostgresProxyDataSource) dataSource).getDatabase();

        Mockito.verify(databasePool).getOrCreateDatabase(eq(PostgresDBType.INSTANCE), any(DbaasDbClassifier.class), any(), connectorSettingsCapture.capture());
        assertSame(flywayRunner, connectorSettingsCapture.getValue().getFlywayRunner());
    }

    @Test
    public void testWithCustomDiscriminator() {
        DataSource dataSource = datasourceBuilder.newBuilder(new ServiceDbaaSClassifierBuilder())
                .withDiscriminator("custom-discriminator")
                .build();

        ((DbaasPostgresProxyDataSource) dataSource).getDatabase();

        Mockito.verify(databasePool).getOrCreateDatabase(eq(PostgresDBType.INSTANCE), any(DbaasDbClassifier.class), any(), connectorSettingsCapture.capture());
        assertEquals("custom-discriminator", connectorSettingsCapture.getValue().getDiscriminator().getValue());
    }

    @Test
    public void testWithRoReplica() {
        DataSource dataSource = datasourceBuilder.newBuilder(new ServiceDbaaSClassifierBuilder())
                .withRoReplica()
                .build();
        ((DbaasPostgresProxyDataSource) dataSource).getDatabase();

        Mockito.verify(databasePool).getOrCreateDatabase(eq(PostgresDBType.INSTANCE), any(DbaasDbClassifier.class), any(), connectorSettingsCapture.capture());
        assertEquals(true, connectorSettingsCapture.getValue().isRoReplica());
    }

}
