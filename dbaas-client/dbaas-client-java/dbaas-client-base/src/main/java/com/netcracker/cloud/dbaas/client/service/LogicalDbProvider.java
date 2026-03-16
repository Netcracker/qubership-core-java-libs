package com.netcracker.cloud.dbaas.client.service;

import com.netcracker.cloud.dbaas.client.entity.database.AbstractDatabase;
import com.netcracker.cloud.dbaas.client.management.DatabaseConfig;
import com.netcracker.cloud.dbaas.client.management.SupportedDatabaseType;
import org.jetbrains.annotations.Nullable;

import java.util.SortedMap;

public interface LogicalDbProvider<C, D extends AbstractDatabase<?>> extends SupportedDatabaseType<D> {
    @Nullable
    D provide(SortedMap<String, Object> classifier, DatabaseConfig params, String namespace);

    default int order() {
        return 0;
    }

    @Nullable
    C provideConnectionProperty(SortedMap<String, Object> classifier, DatabaseConfig params);

    default void provideDatabaseInfo(D database) {
    }
}
