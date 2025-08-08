package com.netcracker.cloud.dbaas.client.management;


import com.clickhouse.jdbc.JdbcWrapper;
import org.qubership.cloud.dbaas.client.entity.database.ClickhouseDatabase;
import org.qubership.cloud.dbaas.client.entity.database.ClickhouseDatasourceConnectorSettings;
import org.qubership.cloud.dbaas.client.entity.database.type.ClickhouseDBType;
import org.qubership.cloud.dbaas.client.management.classifier.DbaaSClassifierBuilder;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The default clickhouse datasource which created clickhouse db via dbaas client
 */
@Slf4j
public class DbaasClickhouseDatasource extends JdbcWrapper implements DataSource {

    protected DbaaSClassifierBuilder dbClassifierBuilder;
    protected DatabasePool databasePool;
    protected DatabaseConfig databaseConfig;
    protected ClickhouseDatasourceConnectorSettings connectorSettings;

    public DbaasClickhouseDatasource(DbaaSClassifierBuilder dbClassifierBuilder, DatabasePool databasePool, DatabaseConfig databaseConfig) {
        this.dbClassifierBuilder = dbClassifierBuilder;
        this.databasePool = databasePool;
        this.databaseConfig = databaseConfig;
    }

    DbaasClickhouseDatasource setConnectorSettings(ClickhouseDatasourceConnectorSettings connectorSettings) {
        this.connectorSettings = connectorSettings;
        return this;
    }

    public ClickhouseDatabase getDatabase() {
        return databasePool.getOrCreateDatabase(ClickhouseDBType.INSTANCE, dbClassifierBuilder.build(), databaseConfig, connectorSettings);
    }

    public DataSource getInnerDataSource() {
        return getDatabase().getConnectionProperties().getDataSource();
    }

    interface SqlCall<T> {
        T call() throws SQLException;
    }

    protected Connection withPasswordCheck(SqlCall<Connection> connectionProvider) throws SQLException {
        try {
            return connectionProvider.call();
        } catch (SQLException ex) {
            if ("28P01".equalsIgnoreCase(getSQLStateFromException(ex))) { // invalid password
                log.info("DB password has expired try to get a new one");
                DbaasDbClassifier dbaasDbClassifier = dbClassifierBuilder.build();
                databasePool.removeCachedDatabase(ClickhouseDBType.INSTANCE, dbaasDbClassifier);
                databasePool.getOrCreateDatabase(ClickhouseDBType.INSTANCE, dbaasDbClassifier);
                log.info("DB password updated successfully");
                return connectionProvider.call();
            } else {
                log.error("Can not get DB.", ex);
                throw ex;
            }
        }
    }

    protected String getSQLStateFromException(Throwable t) {
        final Set<Throwable> checked = Stream.of(t).collect(Collectors.toSet());
        while (t != null) {
            if (t instanceof SQLException) {
                return ((SQLException) t).getSQLState();
            }
            final Throwable cause = t.getCause();
            // check if cause is the exception we already checked to prevent infinite loop
            if (cause != null && !checked.contains(cause)) {
                t = cause;
                checked.add(t);
            } else {
                t = null;
            }
        }
        return "";
    }

    @Override
    public Connection getConnection() throws SQLException {
        return withPasswordCheck(() -> this.getInnerDataSource().getConnection());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return getConnection();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return this.getInnerDataSource().unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return this.getInnerDataSource().isWrapperFor(iface);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return this.getInnerDataSource().getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        this.getInnerDataSource().setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        this.getInnerDataSource().setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return this.getInnerDataSource().getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return this.getInnerDataSource().getParentLogger();
    }
}
