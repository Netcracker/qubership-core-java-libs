package org.qubership.cloud.core.quarkus.dbaas.datasource;

import org.qubership.cloud.core.quarkus.dbaas.datasource.service.DbaaSPostgresDbCreationService;
import org.qubership.cloud.dbaas.client.entity.database.PostgresqlDiscriminator;
import org.qubership.cloud.dbaas.client.management.DatabaseConfig;
import org.qubership.cloud.dbaas.common.classifier.ServiceClassifierBuilder;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DbaasQuarkusPostgresqlDatasourceBuilderTest {
    @Test
    void testDiscriminator() {
        PostgresqlDiscriminator discriminator = PostgresqlDiscriminator.builder().customDiscriminator("test").build();
        DbaaSPostgresDbCreationService service = Mockito.mock(DbaaSPostgresDbCreationService.class);
        DbaasQuarkusPostgresqlDatasourceBuilder builder = new DbaasQuarkusPostgresqlDatasourceBuilder(service);
        DbaaSDataSource ds = (DbaaSDataSource) builder.newBuilder(new ServiceClassifierBuilder(new HashMap<>())).withDiscriminator("test").build();
        assertEquals(discriminator.getValue(), ds.connectorSettings.getDiscriminator().getValue());
    }

    @Test
    void testDiscriminatorOutOfSchemaAndRole() {
        PostgresqlDiscriminator discriminator = PostgresqlDiscriminator.builder().schema("test-schema").userRole("test-role").build();
        DbaaSPostgresDbCreationService service = Mockito.mock(DbaaSPostgresDbCreationService.class);
        DbaasQuarkusPostgresqlDatasourceBuilder builder = new DbaasQuarkusPostgresqlDatasourceBuilder(service);
        DatabaseConfig config = DatabaseConfig.builder().userRole("test-role").build();
        DbaaSDataSource ds = (DbaaSDataSource) builder.newBuilder(new ServiceClassifierBuilder(new HashMap<>()))
                .withSchema("test-schema")
                .withDatabaseConfig(config).build();
        assertEquals(discriminator.getValue(), ds.connectorSettings.getDiscriminator().getValue());
    }

    @Test
    void testWithConnectionProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("options", "-c timeout=100");

        DbaaSPostgresDbCreationService service = Mockito.mock(DbaaSPostgresDbCreationService.class);
        DbaasQuarkusPostgresqlDatasourceBuilder builder = new DbaasQuarkusPostgresqlDatasourceBuilder(service);
        DbaaSDataSource ds = (DbaaSDataSource) builder.newBuilder(new ServiceClassifierBuilder(new HashMap<>()))
                .withConnectionProperties(properties)
                .build();
        assertEquals(properties, ds.connectorSettings.getConnPropertiesParam());
    }

}