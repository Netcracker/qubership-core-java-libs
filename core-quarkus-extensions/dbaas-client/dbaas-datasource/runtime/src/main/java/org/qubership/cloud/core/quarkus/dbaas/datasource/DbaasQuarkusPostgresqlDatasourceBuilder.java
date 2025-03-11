package org.qubership.cloud.core.quarkus.dbaas.datasource;

import org.qubership.cloud.core.quarkus.dbaas.datasource.service.DbaaSPostgresDbCreationService;
import org.qubership.cloud.dbaas.client.entity.database.DatasourceConnectorSettings;
import org.qubership.cloud.dbaas.client.entity.database.PostgresqlDiscriminator;
import org.qubership.cloud.dbaas.client.management.DatabaseConfig;
import org.qubership.cloud.dbaas.client.management.classifier.DbaaSClassifierBuilder;
import org.qubership.cloud.dbaas.client.service.flyway.FlywayRunner;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

public class DbaasQuarkusPostgresqlDatasourceBuilder {
    private final DbaaSPostgresDbCreationService dataSourceCreationService;

    public DbaasQuarkusPostgresqlDatasourceBuilder(DbaaSPostgresDbCreationService dataSourceCreationService) {
        this.dataSourceCreationService = dataSourceCreationService;
    }

    public DbaasQuarkusPostgresqlDatasourceBuilder.Builder newBuilder(DbaaSClassifierBuilder classifierBuilder) {
        return new DbaasQuarkusPostgresqlDatasourceBuilder.Builder(classifierBuilder);
    }

    public class Builder {
        private String schema;
        private String discriminator;
        private DatabaseConfig databaseConfig = DatabaseConfig.builder().build();
        private Map<String, Object> connPropertiesParam = new HashMap<>();
        private FlywayRunner flywayRunner;
        private final DbaaSClassifierBuilder classifierBuilder;

        private Builder(DbaaSClassifierBuilder classifierBuilder) {
            this.classifierBuilder = classifierBuilder;
        }

        public DbaasQuarkusPostgresqlDatasourceBuilder.Builder withSchema(String schema) {
            this.schema = schema;
            return this;
        }

        public DbaasQuarkusPostgresqlDatasourceBuilder.Builder withDiscriminator(String discriminator) {
            this.discriminator = discriminator;
            return this;
        }

        public DbaasQuarkusPostgresqlDatasourceBuilder.Builder withDatabaseConfig(DatabaseConfig databaseConfig) {
            this.databaseConfig = databaseConfig;
            return this;
        }

        public DbaasQuarkusPostgresqlDatasourceBuilder.Builder withConnectionProperties(Map<String, Object> connPropertiesParam) {
            this.connPropertiesParam = connPropertiesParam;
            return this;
        }

        public DbaasQuarkusPostgresqlDatasourceBuilder.Builder withFlyway(FlywayRunner provider) {
            this.flywayRunner = provider;
            return this;
        }

        public DataSource build() {
            DbaaSDataSource dbaaSDataSource = new DbaaSDataSource(classifierBuilder, dataSourceCreationService, databaseConfig);
            dbaaSDataSource.setConnectorSettings(buildDatasourceConnectorSettings());
            return dbaaSDataSource;
        }

        private DatasourceConnectorSettings buildDatasourceConnectorSettings() {
            return DatasourceConnectorSettings.builder()
                    .discriminator(buildDiscriminator())
                    .schema(this.schema)
                    .connPropertiesParam(connPropertiesParam)
                    .flywayRunner(flywayRunner)
                    .build();
        }

        private PostgresqlDiscriminator buildDiscriminator() {
            return PostgresqlDiscriminator.builder()
                    .customDiscriminator(this.discriminator)
                    .userRole(databaseConfig.getUserRole())
                    .schema(this.schema)
                    .build();
        }
    }
}
