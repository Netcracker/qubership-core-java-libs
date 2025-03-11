package org.qubership.cloud.quarkus.dbaas.opensearch.client;

import org.qubership.cloud.dbaas.client.management.DatabaseConfig;
import org.qubership.cloud.dbaas.client.management.DbaasDbClassifier;
import org.qubership.cloud.dbaas.client.management.classifier.DbaaSClassifierBuilder;
import org.qubership.cloud.dbaas.client.opensearch.AbstractDbaasOpensearchClient;
import org.qubership.cloud.dbaas.client.opensearch.entity.OpensearchIndexConnection;
import org.qubership.cloud.quarkus.dbaas.opensearch.client.service.OpensearchDbaaSApiClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;

@Slf4j
class DbaasOpensearchClientImpl extends AbstractDbaasOpensearchClient {
    private final OpensearchDbaaSApiClient opensearchDbaaSApiClient;
    private final DbaaSClassifierBuilder classifierBuilder;
    private final String delimiter;

    DbaasOpensearchClientImpl(DbaaSClassifierBuilder classifierBuilder, OpensearchDbaaSApiClient opensearchDbaaSApiClient, String delimiter) {
        this.opensearchDbaaSApiClient = opensearchDbaaSApiClient;
        this.classifierBuilder = classifierBuilder;
        this.delimiter = delimiter;
    }

    @Override
    public String getDelimiter() {
        return delimiter;
    }

    @Override
    public OpensearchIndexConnection getOrCreateIndex() {
        return getOrCreateIndexWithPasswordCheck(null);
    }

    @Override
    public OpensearchIndexConnection getOrCreateIndex(DatabaseConfig databaseConfig) {
        return getOrCreateIndexWithPasswordCheck(databaseConfig);
    }

    @SneakyThrows
    private OpensearchIndexConnection getOrCreateIndexWithPasswordCheck(DatabaseConfig databaseConfig) {
        DbaasDbClassifier clf = classifierBuilder.build();
        Callable<OpensearchIndexConnection> connectionProvider;
        if (databaseConfig == null) {
            connectionProvider = () -> opensearchDbaaSApiClient.getOrCreateOpensearchIndex(clf);
        } else {
            connectionProvider = () -> opensearchDbaaSApiClient.getOrCreateOpensearchIndex(databaseConfig, clf);
        }
        Runnable connectionEviction = () -> opensearchDbaaSApiClient.removeCachedDatabase(clf);
        return withPasswordCheck(connectionProvider, connectionEviction);
    }

}
