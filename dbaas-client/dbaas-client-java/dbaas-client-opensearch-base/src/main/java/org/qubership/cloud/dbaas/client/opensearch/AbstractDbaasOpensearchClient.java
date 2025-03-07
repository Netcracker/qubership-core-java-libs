package org.qubership.cloud.dbaas.client.opensearch;

import org.qubership.cloud.dbaas.client.management.DatabaseConfig;
import org.qubership.cloud.dbaas.client.opensearch.entity.OpensearchIndexConnection;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.http.HttpStatus;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.OpenSearchException;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.Callable;

@Slf4j
public abstract class AbstractDbaasOpensearchClient implements DbaasOpensearchClient {

    @Override
    public String normalize(String name) {
        OpensearchIndexConnection connection = getOrCreateIndex();
        return normalize(connection, name);
    }

    @Override
    public String normalize(DatabaseConfig databaseConfig, String name) {
        return normalize(getOrCreateIndex(databaseConfig), name);
    }

    public String normalize(OpensearchIndexConnection connection, String name) {
        if (name.startsWith(connection.getResourcePrefix() + getDelimiter())) {
            return name;
        }
        return connection.getResourcePrefix() + getDelimiter() + name;
    }

    @Override
    public String getPrefix() {
        OpensearchIndexConnection connection = getOrCreateIndex();
        return connection.getResourcePrefix();
    }

    @Override
    public OpenSearchClient getClient() {
        return getOrCreateIndex().getOpenSearchClient();
    }

    @Override
    public OpenSearchClient getClient(DatabaseConfig databaseConfig) {
        return getOrCreateIndex(databaseConfig).getOpenSearchClient();
    }

    public abstract String getDelimiter();
    public abstract OpensearchIndexConnection getOrCreateIndex();
    public abstract OpensearchIndexConnection getOrCreateIndex(DatabaseConfig databaseConfig);

    protected OpensearchIndexConnection withPasswordCheck(Callable<OpensearchIndexConnection> connectionProvider,
                                                          Runnable connectionEviction) throws Exception {
        OpensearchIndexConnection connection = connectionProvider.call();

        if (isUnauthorized(connection)) {
            log.debug("DB password has expired. Trying to get new one");
            connectionEviction.run();
            connection = connectionProvider.call();
            if (isUnauthorized(connection)) {
                throw new IllegalStateException("Authorization to Opensearch has been failed. Check credentials");
            }
            log.debug("DB password updated successfully");
        }

        return connection;
    }

    private boolean isUnauthorized(OpensearchIndexConnection connection) throws IOException {
        try {
            connection.getOpenSearchClient().exists(builder -> builder
                    .id(UUID.randomUUID().toString())
                    .index(connection.getResourcePrefix()));
            // this method does not throw RestStatus.NOT_FOUND exception in case object is not found in opensearch
        } catch (OpenSearchException e) {
            if (HttpStatus.SC_UNAUTHORIZED == e.status()) {
                return true;
            }
            log.error("Exception during checking access to Opensearch: ", e);
            throw e;
        } catch (IOException e) {
            if (e.getMessage().contains("Unauthorized")) {
                return true;
            }
            log.error("Exception during checking access to Opensearch: ", e);
            throw e;
        }

        return false;
    }
}
