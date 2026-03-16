package com.netcracker.cloud.core.quarkus.dbaas.datasource.service;

import com.netcracker.cloud.dbaas.client.entity.database.DatasourceConnectorSettings;
import com.netcracker.cloud.dbaas.client.entity.database.PostgresDatabase;
import com.netcracker.cloud.dbaas.client.management.DatabaseConfig;
import com.netcracker.cloud.dbaas.client.management.DbaasDbClassifier;

public interface DbaaSPostgresDbCreationService {
    PostgresDatabase getOrCreatePostgresDatabase(DbaasDbClassifier classifier);

    PostgresDatabase getOrCreatePostgresDatabase(DbaasDbClassifier classifier, DatasourceConnectorSettings connectorSettings, DatabaseConfig databaseConfig);

    void updatePostgresDatabasesPasswords(DbaasDbClassifier classifier);

    void updatePostgresDatabasesPasswords(DbaasDbClassifier classifier, DatasourceConnectorSettings connectorSettings, DatabaseConfig databaseConfig);
}
