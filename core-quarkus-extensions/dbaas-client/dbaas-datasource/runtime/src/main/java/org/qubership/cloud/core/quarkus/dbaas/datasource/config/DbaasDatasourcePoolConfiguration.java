package org.qubership.cloud.core.quarkus.dbaas.datasource.config;

import org.qubership.cloud.core.quarkus.dbaas.datasource.config.properties.DatasourceProperties;
import io.agroal.api.configuration.AgroalConnectionFactoryConfiguration;
import io.agroal.api.configuration.AgroalConnectionPoolConfiguration;
import io.agroal.api.configuration.supplier.AgroalConnectionFactoryConfigurationSupplier;
import io.quarkus.arc.DefaultBean;
import io.quarkus.runtime.util.StringUtil;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.util.Map;
import java.util.Properties;

@Singleton
public class DbaasDatasourcePoolConfiguration {

    public static final String POOL_CONFIGURATION_FACTORY = "connectionPoolConfigurationFactory";

    public static final String CONNECTION_FACTORY_CONFIGURATION = "connectionFactoryConfiguration";

    private static final String JDBC_OPTIONS_CONFIGURATION = "options";

    @Getter
    @Inject
    DatasourceProperties datasourceProperties;

    @Inject
    TransactionManager transactionManager;

    @Inject
    TransactionSynchronizationRegistry transactionSynchronizationRegistry;

    @Produces
    @Named(POOL_CONFIGURATION_FACTORY)
    @DefaultBean
    public AgroalConnectionPoolConfigurationFactory connectionPoolConfigurationFactory() {
        return new AgroalConnectionPoolConfigurationFactory(datasourceProperties, transactionManager, transactionSynchronizationRegistry);
    }

    @Produces
    @Named(CONNECTION_FACTORY_CONFIGURATION)
    @DefaultBean
    public AgroalConnectionFactoryConfiguration connectionFactoryConfiguration() {
        return new AgroalConnectionFactoryConfigurationSupplier()
                .autoCommit(getDatasourceProperties().jdbc.autoCommit)
                .get();
    }

    public Properties getJdbcProperties(@Nullable String logicalDbName) {
        addIdleInTransactionSessionTimeoutToJdbcProperties(getDatasourceProperties().globalJdbcProperties);

        Properties datasourceProperties = new Properties();
        datasourceProperties.putAll(getDatasourceProperties().globalJdbcProperties);
        if (!StringUtil.isNullOrEmpty(logicalDbName) && getDatasourceProperties().datasources.containsKey(logicalDbName)) {
            datasourceProperties.putAll(getDatasourceProperties().datasources.get(logicalDbName).jdbcProperties);
        }
        return datasourceProperties;
    }

    public Properties getXaProperties(@Nullable String logicalDbName) {
        addIdleInTransactionSessionTimeoutToJdbcProperties(getDatasourceProperties().globalXaProperties);

        Properties properties = new Properties();
        properties.putAll(getDatasourceProperties().globalXaProperties);
        if (!StringUtil.isNullOrEmpty(logicalDbName) && getDatasourceProperties().datasources.containsKey(logicalDbName)) {
            properties.putAll(getDatasourceProperties().datasources.get(logicalDbName).xaProperties);
        }
        return properties;
    }

    public boolean isXa(@Nullable String logicalDbName) {
        if (!StringUtil.isNullOrEmpty(logicalDbName) && getDatasourceProperties().datasources.containsKey(logicalDbName)) {
            return getDatasourceProperties().datasources.get(logicalDbName).xa;
        }
        return getDatasourceProperties().xa;
    }

    protected static class DbConnectionValidator implements AgroalConnectionPoolConfiguration.ConnectionValidator {
        private final int respondTimeoutSeconds;

        public DbConnectionValidator(int respondTimeoutSeconds) {
            this.respondTimeoutSeconds = respondTimeoutSeconds;
        }

        @Override
        public boolean isValid(Connection connection) {
            try {
                return connection.isValid(respondTimeoutSeconds);
            } catch (Throwable t) {
                return false;
            }
        }
    }

    private void addIdleInTransactionSessionTimeoutToJdbcProperties(Map<String, String> jdbcProperties){
        String connectionProperties = jdbcProperties.get(JDBC_OPTIONS_CONFIGURATION);
        if(connectionProperties == null){
            jdbcProperties.put(JDBC_OPTIONS_CONFIGURATION, "-c idle-in-transaction-session-timeout=28800000");
        } else {
            if (!connectionProperties.contains("idle-in-transaction-session-timeout")) {
                String finalVal = connectionProperties.concat(" -c idle-in-transaction-session-timeout=28800000");
                jdbcProperties.put(JDBC_OPTIONS_CONFIGURATION, finalVal);
            }
        }
    }
}
