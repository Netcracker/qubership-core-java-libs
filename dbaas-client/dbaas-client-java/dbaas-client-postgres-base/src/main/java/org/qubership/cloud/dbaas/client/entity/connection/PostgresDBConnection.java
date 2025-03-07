package org.qubership.cloud.dbaas.client.entity.connection;


import javax.sql.DataSource;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Data
@ToString(callSuper = true)
@Slf4j
public class PostgresDBConnection extends DatabaseConnection {
    public static final String SSL_FACTORY_POSTFIX = "sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory";
    public static final String SSL_MODE_VERIFY_FULL_POSTFIX = "sslmode=verify-full";
    public static final String SSL_MODE_REQUIRE_POSTFIX = "sslmode=require";
    private DataSource dataSource;
    private String host;
    private String roHost;
    private boolean tlsNotStrict;

    public PostgresDBConnection(String url, String username, String password, String role) {
        super(url, username, password, role);
    }

    public PostgresDBConnection() {
    }

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
