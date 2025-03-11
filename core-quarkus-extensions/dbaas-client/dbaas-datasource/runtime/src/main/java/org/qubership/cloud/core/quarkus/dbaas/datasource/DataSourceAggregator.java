package org.qubership.cloud.core.quarkus.dbaas.datasource;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.AgroalDataSourceMetrics;
import io.agroal.api.AgroalPoolInterceptor;
import io.agroal.api.configuration.AgroalDataSourceConfiguration;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

@Priority(1)
@Alternative
@ApplicationScoped
@Slf4j
@Named("dbaasDataSourceAggregator")
public class DataSourceAggregator implements AgroalDataSource {
    @Inject
    @Named("serviceDataSource")
    AgroalDataSource serviceDS;

    @Inject
    @Named("tenantDataSource")
    AgroalDataSource tenantDS;

    @ConfigProperty(name = "quarkus.dbaas.datasource.main-type", defaultValue = "tenant")
    String dbaasDsMainType;

    private boolean isServiceDb() {
        return !dbaasDsMainType.equalsIgnoreCase("tenant");
    }

    @Override
    public AgroalDataSourceConfiguration getConfiguration() {
        return isServiceDb() ? serviceDS.getConfiguration() : tenantDS.getConfiguration();
    }

    @Override
    public AgroalDataSourceMetrics getMetrics() {
        return isServiceDb() ? serviceDS.getMetrics() : tenantDS.getMetrics();
    }

    @Override
    @SneakyThrows
    public void flush(FlushMode flushMode) {

    }

    @Override
    public void setPoolInterceptors(Collection<? extends AgroalPoolInterceptor> collection) {
        if (isServiceDb()) {
            serviceDS.setPoolInterceptors(collection);
        } else {
            tenantDS.setPoolInterceptors(collection);
        }
    }

    @Override
    public List<AgroalPoolInterceptor> getPoolInterceptors() {
        return isServiceDb() ? serviceDS.getPoolInterceptors() : tenantDS.getPoolInterceptors();
    }

    @Override
    @SneakyThrows
    public void close() {
        if (isServiceDb()) {
            serviceDS.close();
        } else {
            tenantDS.close();
        }
    }

    @Override
    @SneakyThrows
    public Connection getConnection() throws SQLException {
        return isServiceDb() ? serviceDS.getConnection() : tenantDS.getConnection();
    }

    @Override
    @SneakyThrows
    public Connection getConnection(String username, String password) throws SQLException {
        return isServiceDb()
                ? serviceDS.getConnection(username, password)
                : tenantDS.getConnection(username, password);
    }

    @Override
    public <T> T unwrap(Class<T> aClass) throws SQLException {
        if (isServiceDb()) {
            if (aClass.isInstance(this)) {
                return (T) serviceDS;
            } else {
                return serviceDS.unwrap(aClass);
            }
        } else {
            if (aClass.isInstance(this)) {
                return (T) tenantDS;
            } else {
                return tenantDS.unwrap(aClass);
            }
        }
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return isServiceDb() ? serviceDS.isWrapperFor(iface) : tenantDS.isWrapperFor(iface);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return isServiceDb() ? serviceDS.getLogWriter() : tenantDS.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        if (isServiceDb()) {
            serviceDS.setLogWriter(out);
        } else {
            tenantDS.setLogWriter(out);
        }
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        if (isServiceDb()) {
            serviceDS.setLoginTimeout(seconds);
        } else {
            tenantDS.setLoginTimeout(seconds);
        }
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return isServiceDb() ? serviceDS.getLoginTimeout() : tenantDS.getLoginTimeout();
    }

    @SneakyThrows
    @Override
    public Logger getParentLogger() {
        return isServiceDb() ? serviceDS.getParentLogger() : tenantDS.getParentLogger();
    }
}
