package com.netcracker.cloud.dbaas.client.management;

import com.netcracker.cloud.dbaas.client.entity.database.ClickhouseDatasourceConnectorSettings;
import com.netcracker.cloud.dbaas.client.entity.database.type.ClickhouseDBType;
import com.netcracker.cloud.dbaas.client.management.classifier.ServiceDbaaSClassifierBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class DbaasClickhouseDatasourceBuilderTest {

    @Mock
    private DatabasePool databasePool;

    @InjectMocks
    private DbaasClickhouseDatasourceBuilder datasourceBuilder;
    @Captor
    private ArgumentCaptor<ClickhouseDatasourceConnectorSettings> connectorSettingsCapture;

    @Captor
    private ArgumentCaptor<DatabaseConfig> databaseConfigCapture;


    @Test
    void testWithDatabaseConfig() {
        DatabaseConfig databaseConfig = DatabaseConfig.builder()
                .userRole("testUser")
                .build();

        DataSource dataSource = datasourceBuilder.newBuilder(new ServiceDbaaSClassifierBuilder())
                .withDatabaseConfig(databaseConfig)
                .build();

        ((DbaasClickhouseDatasource) dataSource).getDatabase();

        Mockito.verify(databasePool).getOrCreateDatabase(eq(ClickhouseDBType.INSTANCE), any(DbaasDbClassifier.class), databaseConfigCapture.capture(), connectorSettingsCapture.capture());
        assertEquals("testUser", databaseConfigCapture.getValue().getUserRole());
    }

    @Test
    void testWithDatasourceProperties() {
        Properties connectionProperties = new Properties();
        connectionProperties.put("property1", "value1");
        connectionProperties.put("property2", "value2");

        DataSource dataSource = datasourceBuilder.newBuilder(new ServiceDbaaSClassifierBuilder())
                .withDatasourceProperties(connectionProperties)
                .build();

        ((DbaasClickhouseDatasource) dataSource).getDatabase();

        Mockito.verify(databasePool).getOrCreateDatabase(eq(ClickhouseDBType.INSTANCE), any(DbaasDbClassifier.class), any(), connectorSettingsCapture.capture());
        assertEquals("value1", connectorSettingsCapture.getValue().getDatasourceProperties().get("property1"));
        assertEquals("value2", connectorSettingsCapture.getValue().getDatasourceProperties().get("property2"));
    }

    @Test
    void testWithCustomDiscriminator() {
        DataSource dataSource = datasourceBuilder.newBuilder(new ServiceDbaaSClassifierBuilder())
                .withDiscriminator("custom-discriminator")
                .build();

        ((DbaasClickhouseDatasource) dataSource).getDatabase();

        Mockito.verify(databasePool).getOrCreateDatabase(eq(ClickhouseDBType.INSTANCE), any(DbaasDbClassifier.class), any(), connectorSettingsCapture.capture());
        assertEquals("custom-discriminator", connectorSettingsCapture.getValue().getDiscriminator().getValue());
    }
}
