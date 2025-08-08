package com.netcracker.cloud.dbaas.client.opensearch.service;

import com.netcracker.cloud.dbaas.client.management.DatabaseConfig;
import com.netcracker.cloud.dbaas.client.opensearch.entity.OpensearchIndex;
import com.netcracker.cloud.dbaas.client.opensearch.entity.OpensearchIndexConnection;
import com.netcracker.cloud.dbaas.client.service.LogicalDbProvider;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.SortedMap;

import static com.netcracker.cloud.dbaas.client.opensearch.service.OpensearchLogicalDbProvider.OpensearchConnectionProperty;

public abstract class OpensearchLogicalDbProvider implements LogicalDbProvider<OpensearchConnectionProperty, OpensearchIndex> {
    protected static final String TYPE = "opensearch";

    @Override
    public Class<OpensearchIndex> getSupportedDatabaseType() {
        return OpensearchIndex.class;
    }

    @Override
    public OpensearchIndex provide(SortedMap<String, Object> classifier, DatabaseConfig params, String namespace) {
        OpensearchConnectionProperty connProperty = provideConnectionProperty(classifier, params);
        if (connProperty == null) {
            return null;
        }
        OpensearchIndex database = new OpensearchIndex();
        database.setNamespace(namespace);
        database.setClassifier(classifier);

        provideDatabaseInfo(database);

        OpensearchIndexConnection dbConnProperty = new OpensearchIndexConnection();
        dbConnProperty.setUrl(connProperty.url);
        dbConnProperty.setUsername(connProperty.username);
        dbConnProperty.setPassword(connProperty.password);
        dbConnProperty.setHost(connProperty.host);
        dbConnProperty.setResourcePrefix(connProperty.resourcePrefix);
        dbConnProperty.setRole(connProperty.role);
        dbConnProperty.setPort(connProperty.port);
        dbConnProperty.setTls(connProperty.tls);
        database.setConnectionProperties(dbConnProperty);
        return database;
    }

    @RequiredArgsConstructor
    @Getter
    public static class OpensearchConnectionProperty {
        @NonNull
        private String url;
        @NonNull
        private String username;
        @NonNull
        private String password;
        @Setter
        private boolean tls = false;

        @NonNull
        private String host;

        @NonNull
        private String resourcePrefix;
        @Setter
        private String role;
        @NonNull
        private int port;
    }
}
