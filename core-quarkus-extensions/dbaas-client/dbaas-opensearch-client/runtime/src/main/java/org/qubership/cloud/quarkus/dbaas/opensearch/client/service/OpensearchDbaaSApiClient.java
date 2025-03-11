package org.qubership.cloud.quarkus.dbaas.opensearch.client.service;

import org.qubership.cloud.dbaas.client.entity.connection.DatabaseConnection;
import org.qubership.cloud.dbaas.client.management.DatabaseConfig;
import org.qubership.cloud.dbaas.client.management.DbaasDbClassifier;
import org.qubership.cloud.dbaas.client.opensearch.entity.OpensearchIndexConnection;

public interface OpensearchDbaaSApiClient {
    DatabaseConnection getOpensearchIndex(DbaasDbClassifier classifier);

    OpensearchIndexConnection getOrCreateOpensearchIndex(DbaasDbClassifier classifier);

    OpensearchIndexConnection getOrCreateOpensearchIndex(DatabaseConfig dbCreateParameters, DbaasDbClassifier classifier);

    void removeCachedDatabase(DbaasDbClassifier classifier);
}
