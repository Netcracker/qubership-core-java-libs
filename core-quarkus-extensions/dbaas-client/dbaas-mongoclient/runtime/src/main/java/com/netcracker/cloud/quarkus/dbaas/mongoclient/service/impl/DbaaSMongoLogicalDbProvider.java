package com.netcracker.cloud.quarkus.dbaas.mongoclient.service.impl;

import com.netcracker.cloud.dbaas.client.DbaasClient;
import com.netcracker.cloud.dbaas.client.management.DatabaseConfig;
import com.netcracker.cloud.quarkus.dbaas.mongoclient.entity.database.MongoDatabase;
import com.netcracker.cloud.quarkus.dbaas.mongoclient.entity.database.type.MongoDBType;
import com.netcracker.cloud.quarkus.dbaas.mongoclient.service.MongoLogicalDbProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.SortedMap;

/**
 * Default {@link MongoLogicalDbProvider} that resolves the database from dbaas-aggregator over REST.
 * It is last in the chain ({@code order()=Integer.MAX_VALUE}) and reproduces the historical
 * resolution path — {@code dbaasClient.getOrCreateDatabase(MongoDBType.INSTANCE, …)} — so behaviour
 * is unchanged when no other provider resolves the database.
 */
@ApplicationScoped
public class DbaaSMongoLogicalDbProvider extends MongoLogicalDbProvider {

    final DbaasClient dbaasClient;

    @Inject
    public DbaaSMongoLogicalDbProvider(DbaasClient dbaasClient) {
        this.dbaasClient = dbaasClient;
    }

    @Override
    public MongoDatabase provide(SortedMap<String, Object> classifier, DatabaseConfig params, String namespace) {
        return dbaasClient.getOrCreateDatabase(MongoDBType.INSTANCE, namespace, classifier, params);
    }

    @Override
    public int order() {
        return Integer.MAX_VALUE;
    }
}
