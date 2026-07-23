package com.netcracker.cloud.core.quarkus.dbaas.datasource.service.impl;

import com.netcracker.cloud.dbaas.client.entity.database.PostgresDatabase;
import com.netcracker.cloud.dbaas.client.management.DatabaseConfig;
import com.netcracker.cloud.dbaas.client.service.PostgresqlLogicalDbProvider;
import com.netcracker.cloud.dbaas.common.mountedsecret.MountedSecretConnectionSource;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.Nullable;

import java.util.SortedMap;

/**
 * File-backed {@link PostgresqlLogicalDbProvider} that resolves a postgres database from a mounted
 * Secret instead of dbaas REST. It sits in the provider chain just below the agent provider
 * ({@link DbaaSPgLogicalDbProvider}, {@code order()=Integer.MAX_VALUE}): on a mount hit it builds the
 * typed database via the synthetic-response helper; on a miss it returns {@code null} so the chain
 * falls through to the agent provider (REST). Always present — a no-op when
 * {@code /etc/secrets/dbaas-secrets} is empty/absent, so existing behaviour is unchanged.
 */
@ApplicationScoped
public class DbaaSPgMountedSecretLogicalDbProvider extends PostgresqlLogicalDbProvider {

    // After any user-supplied providers (default order 0), just before the agent provider (MAX_VALUE).
    static final int MOUNTED_SECRET_ORDER = Integer.MAX_VALUE - 1;

    private final MountedSecretConnectionSource mountedSecretConnectionSource;

    public DbaaSPgMountedSecretLogicalDbProvider(MountedSecretConnectionSource mountedSecretConnectionSource) {
        this.mountedSecretConnectionSource = mountedSecretConnectionSource;
    }

    @Override
    public int order() {
        return MOUNTED_SECRET_ORDER;
    }

    @Override
    public PostgresDatabase provide(SortedMap<String, Object> classifier, DatabaseConfig config, String namespace) {
        String role = config != null ? config.getUserRole() : null;
        return mountedSecretConnectionSource.resolve(classifier, TYPE, role)
                .map(resolved -> mountedSecretConnectionSource.buildDatabase(PostgresDatabase.class, classifier, resolved))
                .orElse(null);
    }

    @Override
    public @Nullable PostgresConnectionProperty provideConnectionProperty(SortedMap<String, Object> classifier, DatabaseConfig databaseConfig) {
        throw new NotImplementedException();
    }
}
