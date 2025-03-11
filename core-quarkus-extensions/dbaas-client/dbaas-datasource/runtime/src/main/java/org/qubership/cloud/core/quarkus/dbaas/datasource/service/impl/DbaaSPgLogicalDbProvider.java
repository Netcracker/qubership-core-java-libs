package org.qubership.cloud.core.quarkus.dbaas.datasource.service.impl;

import org.qubership.cloud.dbaas.client.DbaasClient;
import org.qubership.cloud.dbaas.client.entity.database.PostgresDatabase;
import org.qubership.cloud.dbaas.client.entity.database.type.PostgresDBType;
import org.qubership.cloud.dbaas.client.management.DatabaseConfig;
import org.qubership.cloud.dbaas.client.service.PostgresqlLogicalDbProvider;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.Nullable;

import java.util.SortedMap;

@ApplicationScoped
public class DbaaSPgLogicalDbProvider extends PostgresqlLogicalDbProvider {
    private DbaasClient dbaasClient;

    public DbaaSPgLogicalDbProvider(DbaasClient dbaaSClient) {
        this.dbaasClient = dbaaSClient;
    }

    public int order() {
        return Integer.MAX_VALUE;
    }

    @Override
    public PostgresDatabase provide(SortedMap<String, Object> classifier, DatabaseConfig config, String namespace) {
        return dbaasClient.getOrCreateDatabase(PostgresDBType.INSTANCE, namespace, classifier, config);
    }

    @Override
    public @Nullable PostgresConnectionProperty provideConnectionProperty(SortedMap<String, Object> classifier, DatabaseConfig databaseConfig) {
        throw new NotImplementedException();
    }

}
