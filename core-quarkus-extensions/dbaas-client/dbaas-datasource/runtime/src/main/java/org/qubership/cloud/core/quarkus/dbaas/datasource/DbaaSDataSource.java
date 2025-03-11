package org.qubership.cloud.core.quarkus.dbaas.datasource;

import org.qubership.cloud.core.quarkus.dbaas.datasource.service.DbaaSPostgresDbCreationService;
import org.qubership.cloud.dbaas.client.entity.database.DatasourceConnectorSettings;
import org.qubership.cloud.dbaas.client.entity.database.PostgresDatabase;
import org.qubership.cloud.dbaas.client.management.DatabaseConfig;
import org.qubership.cloud.dbaas.client.management.classifier.DbaaSClassifierBuilder;
import io.agroal.api.AgroalDataSource;
import io.agroal.api.AgroalDataSourceMetrics;
import io.agroal.api.AgroalPoolInterceptor;
import io.agroal.api.configuration.AgroalDataSourceConfiguration;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

@Slf4j
public class DbaaSDataSource implements AgroalDataSource {

    DbaaSPostgresDbCreationService dataSourceCreationService;
    private DbaaSClassifierBuilder classifierBuilder;
    protected DatasourceConnectorSettings connectorSettings = new DatasourceConnectorSettings();
    private transient DatabaseConfig databaseConfig;


    public DbaaSDataSource(DbaaSClassifierBuilder classifierBuilder,
                           DbaaSPostgresDbCreationService dataSourceCreationService) {
        this.classifierBuilder = classifierBuilder;
        this.dataSourceCreationService = dataSourceCreationService;
    }

    public DbaaSDataSource(DbaaSClassifierBuilder classifierBuilder,
                           DbaaSPostgresDbCreationService dataSourceCreationService,
                           DatabaseConfig databaseConfig) {
        this.classifierBuilder = classifierBuilder;
        this.dataSourceCreationService = dataSourceCreationService;
        this.databaseConfig = databaseConfig;
    }

    DbaaSDataSource setConnectorSettings(DatasourceConnectorSettings connectorSettings) {
        this.connectorSettings = connectorSettings;
        return this;
    }

    public DataSource getInnerDataSource() {
        return getDatasource();
    }

    private PostgresDatabase createPostgresDb()  {
        return dataSourceCreationService.getOrCreatePostgresDatabase(classifierBuilder.build(), connectorSettings, databaseConfig);
    }

    private AgroalDataSource getDatasource() {
        return (AgroalDataSource) createPostgresDb().getConnectionProperties().getDataSource();
    }

    protected Connection withPasswordCheck(Callable<Connection> connectionProvider) throws Exception {
        try {
            return connectionProvider.call();
        } catch (SQLException ex) {
            if ("28P01".equalsIgnoreCase(ex.getSQLState())) { // invalid password
                log.info("DB password has expired try to get a new one");
                dataSourceCreationService.updatePostgresDatabasesPasswords(classifierBuilder.build(), connectorSettings, databaseConfig);
                return connectionProvider.call();
            } else {
                log.error("Can not get DB.", ex);
                throw ex;
            }
        }
    }

    @Override
    public AgroalDataSourceConfiguration getConfiguration() {
        return getDatasource().getConfiguration();
    }

    @Override
    public AgroalDataSourceMetrics getMetrics() {
        return getDatasource().getMetrics();
    }

    @Override
    public void flush(FlushMode flushMode) {
        getDatasource().flush(flushMode);
    }

    @Override
    public void setPoolInterceptors(Collection<? extends AgroalPoolInterceptor> collection) {
        getDatasource().setPoolInterceptors(collection);
    }

    @Override
    public List<AgroalPoolInterceptor> getPoolInterceptors() {
        return getDatasource().getPoolInterceptors();
    }

    @Override
    public void close() {
        getDatasource().close();
    }

    @SneakyThrows
    @Override
    public Connection getConnection() throws SQLException {
        return withPasswordCheck(() -> getDatasource().getConnection());
    }

    @SneakyThrows
    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return withPasswordCheck(() -> getDatasource().getConnection());
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return getDatasource().unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return getDatasource().isWrapperFor(iface);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return getDatasource().getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        getDatasource().setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        getDatasource().setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return getDatasource().getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return getDatasource().getParentLogger();
    }
}
