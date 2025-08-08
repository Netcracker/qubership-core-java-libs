package org.qubership.cloud.dbaas.client.management;

import org.qubership.cloud.dbaas.client.entity.database.ClickhouseDatasourceConnectorSettings;
import org.qubership.cloud.dbaas.client.entity.database.ClickhouseDiscriminator;
import org.qubership.cloud.dbaas.client.management.classifier.DbaaSChainClassifierBuilder;

import javax.sql.DataSource;
import java.util.Properties;

public class DbaasClickhouseDatasourceBuilder {
    private final DatabasePool databasePool;

    public DbaasClickhouseDatasourceBuilder(DatabasePool databasePool) {
        this.databasePool = databasePool;
    }

    public Builder newBuilder(DbaaSChainClassifierBuilder classifierBuilder) {
        return new Builder(classifierBuilder);
    }

    public class Builder {
        private String discriminator;
        private DatabaseConfig databaseConfig = DatabaseConfig.builder().build();
        private final DbaaSChainClassifierBuilder classifierBuilder;
        private Properties datasourceProperties;

        private Builder(DbaaSChainClassifierBuilder classifierBuilder) {
            this.classifierBuilder = classifierBuilder;
        }

        public Builder withDiscriminator(String discriminator) {
            this.discriminator = discriminator;
            return this;
        }

        public Builder withDatabaseConfig(DatabaseConfig databaseConfig) {
            this.databaseConfig = databaseConfig;
            return this;
        }


        public Builder withDatasourceProperties(Properties datasourceProperties) {
            this.datasourceProperties = datasourceProperties;
            return this;
        }

        public DataSource build() {
            DbaasClickhouseDatasource dbaasClickhouseDataSource = new DbaasClickhouseDatasource(classifierBuilder,databasePool, databaseConfig);
            dbaasClickhouseDataSource.setConnectorSettings(buildDatasourceConnectorSettings());
            return dbaasClickhouseDataSource;
        }

        private ClickhouseDatasourceConnectorSettings buildDatasourceConnectorSettings() {
            return ClickhouseDatasourceConnectorSettings.builder()
                    .discriminator(buildDiscriminator())
                    .datasourceProperties(datasourceProperties)
                    .build();
        }

        private ClickhouseDiscriminator buildDiscriminator() {
            return ClickhouseDiscriminator.builder()
                    .customDiscriminator(this.discriminator)
                    .userRole(databaseConfig.getUserRole())
                    .build();
        }
    }
}