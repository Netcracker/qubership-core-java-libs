package com.netcracker.cloud.quarkus.dbaas.cassandraclient.service.impl;

import com.netcracker.cloud.dbaas.client.DbaasClient;
import com.netcracker.cloud.dbaas.client.cassandra.entity.database.CassandraDatabase;
import com.netcracker.cloud.dbaas.client.cassandra.entity.database.type.CassandraDBType;
import com.netcracker.cloud.dbaas.client.cassandra.service.CassandraLogicalDbProvider;
import com.netcracker.cloud.dbaas.client.management.DatabaseConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.Nullable;

import java.util.SortedMap;

@ApplicationScoped
public class DbaaSCassandraLogicalDbProvider extends CassandraLogicalDbProvider {

    @Inject
    DbaasClient dbaasClient;

    @Override
    public CassandraDatabase provide(SortedMap<String, Object> classifier, DatabaseConfig params, String namespace) {

        return dbaasClient.getOrCreateDatabase(CassandraDBType.INSTANCE, namespace, classifier, params);
    }

    @Override
    public int order() {
        return Integer.MAX_VALUE;
    }

    @Override
    public @Nullable CassandraConnectionProperty provideConnectionProperty(SortedMap<String, Object> classifier, DatabaseConfig databaseConfig) {
        throw new NotImplementedException();
    }
}
