package com.netcracker.cloud.quarkus.dbaas.opensearch.client.service.impl;

import com.netcracker.cloud.dbaas.client.DbaasClient;
import com.netcracker.cloud.dbaas.client.management.DatabaseConfig;
import com.netcracker.cloud.dbaas.client.opensearch.entity.OpensearchDBType;
import com.netcracker.cloud.dbaas.client.opensearch.entity.OpensearchIndex;
import com.netcracker.cloud.dbaas.client.opensearch.service.OpensearchLogicalDbProvider;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.Nullable;

import java.util.SortedMap;

@ApplicationScoped
public class DbaaSOpensearchLogicalDbProvider extends OpensearchLogicalDbProvider {

    private DbaasClient dbaasClient;

    public DbaaSOpensearchLogicalDbProvider(DbaasClient dbaasClient) {
        this.dbaasClient = dbaasClient;
    }

    @Override
    public int order() {
        return Integer.MAX_VALUE;
    }

    @Override
    public OpensearchIndex provide(SortedMap<String, Object> classifier, DatabaseConfig params, String namespace) {
        return dbaasClient.getOrCreateDatabase(OpensearchDBType.INSTANCE, namespace, classifier, params);
    }

    @Override
    public @Nullable OpensearchConnectionProperty provideConnectionProperty(SortedMap<String, Object> classifier, DatabaseConfig databaseConfig) {
        throw new NotImplementedException();
    }
}
