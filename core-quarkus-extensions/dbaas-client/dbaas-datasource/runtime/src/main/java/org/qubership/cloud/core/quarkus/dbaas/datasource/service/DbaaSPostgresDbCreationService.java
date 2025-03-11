package org.qubership.cloud.core.quarkus.dbaas.datasource.service;

import org.qubership.cloud.dbaas.client.entity.database.DatasourceConnectorSettings;
import org.qubership.cloud.dbaas.client.entity.database.PostgresDatabase;
import org.qubership.cloud.dbaas.client.management.DatabaseConfig;
import org.qubership.cloud.dbaas.client.management.DbaasDbClassifier;

public interface DbaaSPostgresDbCreationService {
    PostgresDatabase getOrCreatePostgresDatabase(DbaasDbClassifier classifier);

    PostgresDatabase getOrCreatePostgresDatabase(DbaasDbClassifier classifier, DatasourceConnectorSettings connectorSettings, DatabaseConfig databaseConfig);

    void updatePostgresDatabasesPasswords(DbaasDbClassifier classifier);

    void updatePostgresDatabasesPasswords(DbaasDbClassifier classifier, DatasourceConnectorSettings connectorSettings, DatabaseConfig databaseConfig);
}
