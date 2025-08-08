package org.qubership.cloud.dbaas.client.management;

import org.qubership.cloud.dbaas.client.entity.database.DatasourceConnectorSettings;
import org.qubership.cloud.dbaas.client.entity.database.PostgresqlDiscriminator;
import org.qubership.cloud.dbaas.client.management.classifier.DbaaSChainClassifierBuilder;
import org.qubership.cloud.dbaas.client.service.flyway.FlywayRunner;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

public class DbaasPostgresqlDatasourceBuilder {
    private final DatabasePool databasePool;

    public DbaasPostgresqlDatasourceBuilder(DatabasePool databasePool) {
        this.databasePool = databasePool;
    }

    public Builder newBuilder(DbaaSChainClassifierBuilder classifierBuilder) {
        return new Builder(classifierBuilder);
    }

    public class Builder {
        private String schema;
        private String discriminator;
        private DatabaseConfig databaseConfig = DatabaseConfig.builder().build();
        private Map<String, Object> connPropertiesParam = new HashMap<>();
        private FlywayRunner flywayRunner;
        private boolean roReplica;
        private final DbaaSChainClassifierBuilder classifierBuilder;

        private Builder(DbaaSChainClassifierBuilder classifierBuilder) {
            this.classifierBuilder = classifierBuilder;
        }

        public Builder withSchema(String schema) {
            this.schema = schema;
            return this;
        }

        public Builder withDiscriminator(String discriminator) {
            this.discriminator = discriminator;
            return this;
        }

        public Builder withDatabaseConfig(DatabaseConfig databaseConfig) {
            this.databaseConfig = databaseConfig;
            return this;
        }

        public Builder withConnectionProperties(Map<String, Object> connPropertiesParam) {
            this.connPropertiesParam = connPropertiesParam;
            return this;
        }

        public Builder withFlyway(FlywayRunner provider) {
            this.flywayRunner = provider;
            return this;
        }

        public Builder withRoReplica() {
            this.roReplica = true;
            return this;
        }

        public DataSource build() {
            DbaasPostgresProxyDataSource dbaasPostgresProxyDataSource = new DbaasPostgresProxyDataSource(databasePool, classifierBuilder, databaseConfig);
            dbaasPostgresProxyDataSource.setConnectorSettings(buildDatasourceConnectorSettings());
            return dbaasPostgresProxyDataSource;
        }

        private DatasourceConnectorSettings buildDatasourceConnectorSettings() {
            return DatasourceConnectorSettings.builder()
                    .discriminator(buildDiscriminator())
                    .schema(this.schema)
                    .connPropertiesParam(connPropertiesParam)
                    .flywayRunner(flywayRunner)
                    .roReplica(roReplica)
                    .build();
        }

        private PostgresqlDiscriminator buildDiscriminator() {
            return PostgresqlDiscriminator.builder()
                    .customDiscriminator(this.discriminator)
                    .userRole(databaseConfig.getUserRole())
                    .roReplica(this.roReplica)
                    .schema(this.schema)
                    .build();
        }
    }
}
