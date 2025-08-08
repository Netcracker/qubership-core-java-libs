package org.qubership.cloud.dbaas.client.entity.connection;


import com.clickhouse.jdbc.ClickHouseDataSource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class ClickhouseConnection extends DatabaseConnection {

    private String host;
    private int port;
    private ClickHouseDataSource dataSource;

    public ClickhouseConnection(String url, String username, String password, String role) {
        super(url, username, password, role);
    }

    public ClickhouseConnection() {
    }

    @Override
    public void close() throws Exception {
        if (dataSource == null) {
            log.error("Cannot close null datasource for connection to {}", this.getUrl());
            return;
        }
        if (dataSource instanceof AutoCloseable) {
            ((AutoCloseable) dataSource).close();
        }
    }
}
