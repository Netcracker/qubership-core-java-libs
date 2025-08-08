package org.qubership.cloud.dbaas.client.entity.postgres;


import org.qubership.cloud.dbaas.client.entity.connection.DatabaseConnection;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;

@Data
@ToString(of = {"host", "port"}, callSuper = true)
@Slf4j
public class PostgresDBConnection extends DatabaseConnection {
    private String host;
    private int port;
    private DataSource dataSource;

    @Override
    public void close() throws Exception {
        if (dataSource == null) {
            log.error("Cannot close null datasource for connection to {}", this.getUrl());
            return;
        }
        if (dataSource instanceof AutoCloseable) {
            ((AutoCloseable) dataSource).close();
        } else {
            log.error("Cannot close connection to {} because of incorrect not " +
                            "autoclosable data source realization of type {}",
                    this.getUrl(), this.getDataSource().getClass().getName());
        }
    }
}
