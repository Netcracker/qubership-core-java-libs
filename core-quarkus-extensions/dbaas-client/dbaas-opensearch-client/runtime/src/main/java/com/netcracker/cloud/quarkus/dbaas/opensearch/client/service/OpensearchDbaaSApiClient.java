package com.netcracker.cloud.quarkus.dbaas.opensearch.client.service;

import com.netcracker.cloud.dbaas.client.entity.connection.DatabaseConnection;
import com.netcracker.cloud.dbaas.client.management.DatabaseConfig;
import com.netcracker.cloud.dbaas.client.management.DbaasDbClassifier;
import com.netcracker.cloud.dbaas.client.opensearch.entity.OpensearchIndexConnection;

public interface OpensearchDbaaSApiClient {
    DatabaseConnection getOpensearchIndex(DbaasDbClassifier classifier);

    OpensearchIndexConnection getOrCreateOpensearchIndex(DbaasDbClassifier classifier);

    OpensearchIndexConnection getOrCreateOpensearchIndex(DatabaseConfig dbCreateParameters, DbaasDbClassifier classifier);

    void removeCachedDatabase(DbaasDbClassifier classifier);
}
