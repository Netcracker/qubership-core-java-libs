package org.qubership.cloud.dbaas.client.entity.database;

import com.clickhouse.client.config.ClickHouseDefaults;
import com.clickhouse.jdbc.ClickHouseDataSource;
import org.qubership.cloud.dbaas.client.entity.connection.ClickhouseConnection;
import org.qubership.cloud.dbaas.client.management.DatabaseClientCreator;
import lombok.extern.slf4j.Slf4j;

import java.sql.SQLException;
import java.util.Optional;
import java.util.Properties;


@Slf4j
public class ClickhouseDatasourceCreator implements DatabaseClientCreator<ClickhouseDatabase, ClickhouseDatasourceConnectorSettings> {

    private DbaasClickhouseDatasourceProperties datasourceProperties;

    public ClickhouseDatasourceCreator(DbaasClickhouseDatasourceProperties dsProperties) {
        this.datasourceProperties = dsProperties;
    }

    @Override
    public void create(ClickhouseDatabase database){
        create(database, new ClickhouseDatasourceConnectorSettings());
    }

    @Override
    public void create(ClickhouseDatabase database, ClickhouseDatasourceConnectorSettings settings){
        if (settings == null) {
            settings = new ClickhouseDatasourceConnectorSettings();
        }
        log.debug("Starting the initialization of DataSource for database: {}", database);
        ClickhouseConnection connectionProperties = database.getConnectionProperties();

        String password = connectionProperties.getPassword();
        log.debug("use password from dbaas storage");

        Properties properties = new Properties();
        if (settings.getDatasourceProperties() != null) {
            properties.putAll(settings.getDatasourceProperties());
        }
        else if (isNotEmpty(datasourceProperties)) {
            properties.putAll(datasourceProperties.getDatasourceProperties());
        }

        properties.setProperty(ClickHouseDefaults.USER.getKey(), connectionProperties.getUsername());
        properties.setProperty(ClickHouseDefaults.PASSWORD.getKey(), password);
        String url = connectionProperties.getUrl();
        if (!url.contains("jdbc:")) {
            url = "jdbc:" + url;
        }
        String port = String.valueOf(connectionProperties.getPort());
        if (connectionProperties.isTls()) {
            log.info("Connection to clickhouse will be secured");
            properties.setProperty("ssl", "true");
            url = url.replaceFirst(port, "8443");
        }
        else {
            url = url.replaceFirst(port, "8123");
        }

        try {
            ClickHouseDataSource clickHouseDataSource = new ClickHouseDataSource(url, properties);
            log.info("dataSource created for {}", database);
            connectionProperties.setDataSource(clickHouseDataSource);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private boolean isNotEmpty(DbaasClickhouseDatasourceProperties datasourceProperties) {
        return datasourceProperties.getDatasourceProperties() != null && !datasourceProperties.getDatasourceProperties().isEmpty();
    }

    @Override
    public Class<ClickhouseDatabase> getSupportedDatabaseType() {
        return ClickhouseDatabase.class;
    }
}
