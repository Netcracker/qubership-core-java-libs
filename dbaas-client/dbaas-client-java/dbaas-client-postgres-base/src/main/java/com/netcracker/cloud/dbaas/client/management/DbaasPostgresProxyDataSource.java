package com.netcracker.cloud.dbaas.client.management;

import org.qubership.cloud.dbaas.client.entity.database.DatasourceConnectorSettings;
import org.qubership.cloud.dbaas.client.entity.database.PostgresDatabase;
import org.qubership.cloud.dbaas.client.entity.database.type.PostgresDBType;
import org.qubership.cloud.dbaas.client.management.classifier.DbaaSClassifierBuilder;
import org.qubership.cloud.dbaas.client.management.classifier.ServiceDbaaSClassifierBuilder;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * DataSource wrapper which delegates its invocations to the datasource of the particular database which is cached in the databasePool
 */
@Slf4j
public class DbaasPostgresProxyDataSource implements DataSource {

    private final DatabaseConfig databaseConfig;
    protected DatabasePool databasePool;
    protected final DbaaSClassifierBuilder builder;

    protected DatasourceConnectorSettings connectorSettings;

    public DbaasPostgresProxyDataSource(DatabasePool databasePool) {
        this(databasePool, new ServiceDbaaSClassifierBuilder(null), DatabaseConfig.builder().build());
    }

    public DbaasPostgresProxyDataSource(DatabasePool databasePool,
                                        DbaaSClassifierBuilder builder,
                                        DatabaseConfig databaseConfig) {
        Objects.requireNonNull(databasePool);
        this.databaseConfig = databaseConfig;
        this.databasePool = databasePool;
        this.builder = builder;
    }

    DbaasPostgresProxyDataSource setConnectorSettings(DatasourceConnectorSettings connectorSettings) {
        this.connectorSettings = connectorSettings;
        return this;
    }


    public PostgresDatabase getDatabase() {
        DbaasDbClassifier dbaasDbClassifier = builder.build();
        return databasePool.getOrCreateDatabase(PostgresDBType.INSTANCE, dbaasDbClassifier, databaseConfig, connectorSettings);
    }

    public DataSource getInnerDataSource() {
        return getDatabase().getConnectionProperties().getDataSource();
    }

    protected Connection withPasswordCheck(SqlCall<Connection> connectionProvider) throws SQLException {
        try {
            return connectionProvider.call();
        } catch (SQLException ex) {
            if ("28P01".equalsIgnoreCase(getSQLStateFromException(ex))) { // invalid password
                log.info("DB password has expired try to get a new one");
                DbaasDbClassifier dbaasDbClassifier = builder.build();
                databasePool.removeCachedDatabase(PostgresDBType.INSTANCE, dbaasDbClassifier);
                databasePool.getOrCreateDatabase(PostgresDBType.INSTANCE, dbaasDbClassifier);
                log.info("DB password updated successfully");
                return connectionProvider.call();
            } else {
                log.error("Can not get DB.", ex);
                throw ex;
            }
        }
    }

    interface SqlCall<T> {
        T call() throws SQLException;
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
        return withPasswordCheck(() -> this.getInnerDataSource().getConnection(username, password));
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return this.getInnerDataSource().unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        if(iface.getSimpleName().equals("AbstractRoutingDataSource"))
            return false;
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
