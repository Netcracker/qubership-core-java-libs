package com.netcracker.cloud.dbaas.client.service;

import org.qubership.cloud.dbaas.client.entity.connection.PostgresDBConnection;
import org.qubership.cloud.dbaas.client.entity.database.PostgresDatabase;
import org.qubership.cloud.dbaas.client.management.DatabaseConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.postgresql.Driver;
import org.postgresql.PGProperty;

import java.util.Properties;
import java.util.SortedMap;

import static org.qubership.cloud.dbaas.client.service.PostgresqlLogicalDbProvider.PostgresConnectionProperty;

@Slf4j
public abstract class PostgresqlLogicalDbProvider implements LogicalDbProvider<PostgresConnectionProperty, PostgresDatabase> {
    protected static final String TYPE = "postgresql";

    @Override
    public Class<PostgresDatabase> getSupportedDatabaseType() {
        return PostgresDatabase.class;
    }

    @Override
    public PostgresDatabase provide(SortedMap<String, Object> classifier, DatabaseConfig params, String namespace) {
        PostgresConnectionProperty pgProperty = provideConnectionProperty(classifier, params);
        if (pgProperty == null) {
            return null;
        }
        PostgresDatabase postgresDatabase = new PostgresDatabase();
        postgresDatabase.setNamespace(namespace);
        postgresDatabase.setClassifier(classifier);
        provideDatabaseInfo(postgresDatabase);
        if (postgresDatabase.getName() == null) {
            postgresDatabase.setName(parseDatabaseName(pgProperty.url));
        }

        PostgresDBConnection postgresDBConnection = new PostgresDBConnection(
                pgProperty.url,
                pgProperty.username,
                pgProperty.password,
                pgProperty.role
        );
        postgresDBConnection.setTls(pgProperty.tls);
        postgresDatabase.setConnectionProperties(postgresDBConnection);
        return postgresDatabase;
    }

    public String parseDatabaseName(String url) {
        Properties parsedUrlProperties = Driver.parseURL(url, new Properties());
        if (parsedUrlProperties == null) {
            log.warn("Cannot parse postgresql connection url");
            return null;
        } else {
            return PGProperty.PG_DBNAME.getSetString(parsedUrlProperties);
        }
    }

    @RequiredArgsConstructor
    @AllArgsConstructor
    @Getter
    public static class PostgresConnectionProperty {
        @NonNull
        private String url;
        @NonNull
        private String username;
        @NonNull
        private String password;
        private String role;
        private boolean tls;

        public PostgresConnectionProperty(@NonNull String url, @NonNull String username, @NonNull String password, String role) {
            this.url = url;
            this.username = username;
            this.password = password;
            this.role = role;
        }
    }
}
