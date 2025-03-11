package org.qubership.cloud.core.quarkus.dbaas.datasource.service.agroal;

import io.agroal.api.configuration.AgroalConnectionFactoryConfiguration;
import io.agroal.api.security.AgroalSecurityProvider;

import java.security.Principal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

import static io.agroal.api.configuration.AgroalConnectionFactoryConfiguration.TransactionIsolation.UNDEFINED;


/**
 * This class used for rewriting default values supplied by AgroalConnectionFactoryConfigurationSupplier.
 */
class AgroalConnectionFactoryConfigurationHolder implements AgroalConnectionFactoryConfiguration {
    // Here is a list of default values copied from AgroalConnectionFactoryConfigurationSupplier
    boolean autoCommit = true;
    boolean trackJdbcResources = true;
    String jdbcUrl = "";
    String initialSql = "";
    IsolationLevel transactionIsolation = UNDEFINED;
    Principal principal;
    Principal recoveryPrincipal;
    Properties jdbcProperties = new Properties();
    Properties xaProperties = new Properties();
    Collection<Object> recoveryCredentials = new ArrayList<>();
    Collection<Object> credentials = new ArrayList<>();
    Duration loginTimeout = Duration.ZERO;

    // Here is a list of changed default values
    Class<?> connectionProviderClass = org.postgresql.Driver.class;
    Collection<AgroalSecurityProvider> securityProviders = new ArrayList<>();

    @Override
    public boolean autoCommit() {
        return autoCommit;
    }

    @Override
    public boolean trackJdbcResources() {
        return trackJdbcResources;
    }

    @Override
    public Duration loginTimeout() {
        return loginTimeout;
    }

    @Override
    public String jdbcUrl() {
        return jdbcUrl;
    }

    @Override
    public String initialSql() {
        return initialSql;
    }

    @Override
    public Class<?> connectionProviderClass() {
        return connectionProviderClass;
    }

    @Override
    public IsolationLevel jdbcTransactionIsolation() {
        return transactionIsolation;
    }

    @Override
    public Collection<AgroalSecurityProvider> securityProviders() {
        return securityProviders;
    }

    @Override
    public Principal principal() {
        return principal;
    }

    @Override
    public Collection<Object> credentials() {
        return credentials;
    }

    @Override
    public Principal recoveryPrincipal() {
        return recoveryPrincipal;
    }

    @Override
    public Collection<Object> recoveryCredentials() {
        return recoveryCredentials;
    }

    @Override
    public Properties jdbcProperties() {
        return jdbcProperties;
    }

    @Override
    public Properties xaProperties() {
        return xaProperties;
    }
}
