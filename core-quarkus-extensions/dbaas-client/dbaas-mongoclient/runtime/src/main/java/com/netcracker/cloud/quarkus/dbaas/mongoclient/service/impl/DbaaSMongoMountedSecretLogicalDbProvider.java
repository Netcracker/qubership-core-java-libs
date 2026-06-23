package com.netcracker.cloud.quarkus.dbaas.mongoclient.service.impl;

import com.netcracker.cloud.dbaas.client.management.DatabaseConfig;
import com.netcracker.cloud.dbaas.common.mountedsecret.MountedSecretConnectionSource;
import com.netcracker.cloud.quarkus.dbaas.mongoclient.entity.database.MongoDatabase;
import com.netcracker.cloud.quarkus.dbaas.mongoclient.service.MongoLogicalDbProvider;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.SortedMap;

/**
 * File-backed {@link MongoLogicalDbProvider} that resolves a mongo database from a mounted Secret
 * instead of dbaas REST. It sits in the chain just below the agent provider
 * ({@link DbaaSMongoLogicalDbProvider}, {@code order()=Integer.MAX_VALUE}): on a mount hit it builds
 * {@link MongoDatabase} via the synthetic-response helper; on a miss it returns {@code null} so the
 * chain falls through to the agent provider (REST). Always present — a no-op when
 * {@code /etc/secrets/dbaas-secrets} is empty/absent, so existing behaviour is unchanged.
 */
@ApplicationScoped
public class DbaaSMongoMountedSecretLogicalDbProvider extends MongoLogicalDbProvider {

    // After any user-supplied providers (default order 0), just before the agent provider (MAX_VALUE).
    static final int MOUNTED_SECRET_ORDER = Integer.MAX_VALUE - 1;

    private final MountedSecretConnectionSource mountedSecretConnectionSource;

    public DbaaSMongoMountedSecretLogicalDbProvider(MountedSecretConnectionSource mountedSecretConnectionSource) {
        this.mountedSecretConnectionSource = mountedSecretConnectionSource;
    }

    @Override
    public int order() {
        return MOUNTED_SECRET_ORDER;
    }

    @Override
    public MongoDatabase provide(SortedMap<String, Object> classifier, DatabaseConfig config, String namespace) {
        String role = config != null ? config.getUserRole() : null;
        return mountedSecretConnectionSource.resolve(classifier, TYPE, role)
                .map(resolved -> mountedSecretConnectionSource.buildDatabase(MongoDatabase.class, classifier, resolved))
                .orElse(null);
    }
}
