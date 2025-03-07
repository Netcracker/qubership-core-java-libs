package org.qubership.cloud.dbaas.client.opensearch;

import org.qubership.cloud.dbaas.client.management.DatabaseConfig;
import org.opensearch.client.opensearch.OpenSearchClient;

public interface DbaasOpensearchClient {

    OpenSearchClient getClient();

    OpenSearchClient getClient(DatabaseConfig databaseConfig);

    String getPrefix();

    String normalize(String name);

    String normalize(DatabaseConfig databaseConfig, String name);
}
