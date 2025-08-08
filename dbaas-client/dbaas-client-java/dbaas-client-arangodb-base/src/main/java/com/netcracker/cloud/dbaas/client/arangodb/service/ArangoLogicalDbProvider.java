package com.netcracker.cloud.dbaas.client.arangodb.service;

import org.qubership.cloud.dbaas.client.arangodb.entity.connection.ArangoConnection;
import org.qubership.cloud.dbaas.client.arangodb.entity.database.ArangoDatabase;
import org.qubership.cloud.dbaas.client.management.DatabaseConfig;
import org.qubership.cloud.dbaas.client.service.LogicalDbProvider;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.SortedMap;

import static org.qubership.cloud.dbaas.client.arangodb.service.ArangoLogicalDbProvider.ArangoConnectionProperty;

public abstract class ArangoLogicalDbProvider implements LogicalDbProvider<ArangoConnectionProperty, ArangoDatabase> {
    protected static final String TYPE = "arangodb";


    @Override
    public Class<ArangoDatabase> getSupportedDatabaseType() {
        return ArangoDatabase.class;
    }

    @Override
    public final ArangoDatabase provide(SortedMap<String, Object> classifier, DatabaseConfig params, String namespace) {
        ArangoConnectionProperty connProperty = provideConnectionProperty(classifier, params);
        if (connProperty == null) {
            return null;
        }
        ArangoDatabase database = new ArangoDatabase();
        database.setNamespace(namespace);
        database.setClassifier(classifier);

        provideDatabaseInfo(database);

        ArangoConnection dbConnProperty = new ArangoConnection();
        dbConnProperty.setUrl(connProperty.url);
        dbConnProperty.setUsername(connProperty.username);
        dbConnProperty.setPassword(connProperty.password);
        dbConnProperty.setDbName(connProperty.dbName);
        dbConnProperty.setRole(connProperty.role);
        dbConnProperty.setHost(connProperty.host);
        dbConnProperty.setPort(connProperty.port);

        database.setConnectionProperties(dbConnProperty);
        return database;
    }

    @AllArgsConstructor
    @Getter
    public static class ArangoConnectionProperty {
        private String host;
        private int port;
        private String dbName;
        protected String url;
        private String username;
        private String password;
        private String role;
    }
}
