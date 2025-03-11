package org.qubership.cloud.core.quarkus.dbaas.datasource.service.agroal;

import io.agroal.api.configuration.AgroalConnectionFactoryConfiguration;
import io.agroal.api.security.AgroalSecurityProvider;

import java.security.Principal;
import java.util.Properties;

public class AgroalConnectionFactoryConfigurationBuilder {

    private final AgroalConnectionFactoryConfigurationHolder configurationHolder = new AgroalConnectionFactoryConfigurationHolder();

    public AgroalConnectionFactoryConfigurationBuilder jdbcUrl(String jdbcUrl) {
        configurationHolder.jdbcUrl = jdbcUrl;
        return this;
    }

    public AgroalConnectionFactoryConfigurationBuilder principal(Principal principal) {
        configurationHolder.principal = principal;
        return this;
    }

    public AgroalConnectionFactoryConfigurationBuilder credential(Object credential) {
        configurationHolder.credentials.add(credential);
        return this;
    }

    public AgroalConnectionFactoryConfigurationBuilder connectionProviderClass(Class<?> connectionProvider) {
        configurationHolder.connectionProviderClass = connectionProvider;
        return this;
    }

    public AgroalConnectionFactoryConfigurationBuilder securityProvider(AgroalSecurityProvider securityProvider) {
        configurationHolder.securityProviders.add(securityProvider);
        return this;
    }

    public AgroalConnectionFactoryConfigurationBuilder autoCommit(boolean autoCommit) {
        configurationHolder.autoCommit = autoCommit;
        return this;
    }

    public AgroalConnectionFactoryConfigurationBuilder jdbcProperties(Properties jdbcProperties) {
        configurationHolder.jdbcProperties = jdbcProperties;
        return this;
    }

    public AgroalConnectionFactoryConfigurationBuilder xaProperties(Properties xaProperties) {
        configurationHolder.xaProperties = xaProperties;
        return this;
    }

    public AgroalConnectionFactoryConfiguration build() {
        return configurationHolder;
    }
}
