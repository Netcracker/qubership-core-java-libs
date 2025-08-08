package com.netcracker.cloud.dbaas.client.cassandra.service;

import org.qubership.cloud.dbaas.client.cassandra.entity.connection.CassandraDBConnection;
import org.qubership.cloud.dbaas.client.cassandra.entity.database.CassandraDatabase;
import org.qubership.cloud.dbaas.client.management.DatabaseConfig;
import org.qubership.cloud.dbaas.client.service.LogicalDbProvider;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.SortedMap;

import static org.qubership.cloud.dbaas.client.cassandra.service.CassandraLogicalDbProvider.CassandraConnectionProperty;
import static org.qubership.cloud.dbaas.client.entity.database.type.PhysicalDbType.CASSANDRA;

public abstract class CassandraLogicalDbProvider implements LogicalDbProvider<CassandraConnectionProperty, CassandraDatabase> {
    protected static final String TYPE = CASSANDRA;

    @Override
    public Class<CassandraDatabase> getSupportedDatabaseType() {
        return CassandraDatabase.class;
    }

    @Override
    public CassandraDatabase provide(SortedMap<String, Object> classifier, DatabaseConfig params, String namespace) {
        CassandraConnectionProperty connProperty = provideConnectionProperty(classifier, params);
        if (connProperty == null) {
            return null;
        }
        CassandraDatabase database = new CassandraDatabase();
        database.setNamespace(namespace);
        database.setClassifier(classifier);

        provideDatabaseInfo(database);

        CassandraDBConnection dbConnProperty = new CassandraDBConnection();
        dbConnProperty.setUrl(connProperty.url);
        dbConnProperty.setUsername(connProperty.username);
        dbConnProperty.setPassword(connProperty.password);
        dbConnProperty.setContactPoints(connProperty.contactPoints);
        dbConnProperty.setKeyspace(connProperty.keyspace);
        dbConnProperty.setRole(connProperty.role);
        dbConnProperty.setPort(connProperty.port);
        dbConnProperty.setTls(connProperty.tls);

        database.setConnectionProperties(dbConnProperty);
        return database;
    }


    @AllArgsConstructor
    @Getter
    public static class CassandraConnectionProperty {
        private String url;
        private String username;
        private String password;
        private List<String> contactPoints;
        private String keyspace;
        private int port;
        private String role;
        private boolean tls;
    }
}
