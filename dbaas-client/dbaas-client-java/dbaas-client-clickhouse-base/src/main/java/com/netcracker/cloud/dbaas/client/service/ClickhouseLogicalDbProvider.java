package org.qubership.cloud.dbaas.client.service;

import com.clickhouse.jdbc.internal.ClickHouseJdbcUrlParser;
import org.qubership.cloud.dbaas.client.entity.connection.ClickhouseConnection;
import org.qubership.cloud.dbaas.client.entity.database.ClickhouseDatabase;
import org.qubership.cloud.dbaas.client.management.DatabaseConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.sql.SQLException;
import java.util.Properties;
import java.util.SortedMap;

@Slf4j
public abstract class ClickhouseLogicalDbProvider implements LogicalDbProvider<ClickhouseLogicalDbProvider.ClickhouseConnectionProperty, ClickhouseDatabase> {
    @Override
    public Class<ClickhouseDatabase> getSupportedDatabaseType() {
        return ClickhouseDatabase.class;
    }

    @Override
    public ClickhouseDatabase provide(SortedMap<String, Object> classifier, DatabaseConfig params, String namespace) {
        ClickhouseConnectionProperty connProperty = provideConnectionProperty(classifier, params);
        if (connProperty == null) {
            return null;
        }
        ClickhouseDatabase database = new ClickhouseDatabase();
        database.setNamespace(namespace);
        database.setClassifier(classifier);

        provideDatabaseInfo(database);
        if (database.getName() == null) {
            try {
                database.setName(parseDatabaseName(connProperty.url));
            }
            catch (SQLException e){
                log.error("Cannot parse database name. SQL exception was thrown");
                throw new RuntimeException(e.getMessage());
            }
        }

        ClickhouseConnection dbConnProperty = new ClickhouseConnection();
        dbConnProperty.setUrl(connProperty.url);
        dbConnProperty.setUsername(connProperty.username);
        dbConnProperty.setPassword(connProperty.password);
        dbConnProperty.setRole(connProperty.role);
        dbConnProperty.setHost(connProperty.host);
        dbConnProperty.setPort(connProperty.port);
        dbConnProperty.setTls(connProperty.tls);
        database.setConnectionProperties(dbConnProperty);

        database.setConnectionProperties(dbConnProperty);
        return database;
    }

    public String parseDatabaseName(String url) throws SQLException {
        Properties parsedUrlProperties = ClickHouseJdbcUrlParser.parse(url, new Properties()).getProperties();
        if (parsedUrlProperties == null) {
            log.warn("Cannot parse clickhouse connection url");
            return null;
        } else {
            return parsedUrlProperties.getProperty("database");
        }
    }

    @AllArgsConstructor
    @Getter
    public static class ClickhouseConnectionProperty {
        private String host;
        private int port;
        private String dbName;
        protected String url;
        private String username;
        private String password;
        private String role;
        private boolean tls;

        public ClickhouseConnectionProperty(@NonNull String url, @NonNull String username, @NonNull String password, String role) {
            this.url = url;
            this.username = username;
            this.password = password;
            this.role = role;
        }
    }
}