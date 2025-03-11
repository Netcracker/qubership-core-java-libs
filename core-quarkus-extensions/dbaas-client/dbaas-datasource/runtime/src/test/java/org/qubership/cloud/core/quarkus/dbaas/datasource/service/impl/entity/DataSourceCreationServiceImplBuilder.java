package org.qubership.cloud.core.quarkus.dbaas.datasource.service.impl.entity;

import org.qubership.cloud.core.quarkus.dbaas.datasource.config.AgroalConnectionPoolConfigurationFactory;
import org.qubership.cloud.core.quarkus.dbaas.datasource.config.DbaasDatasourcePoolConfiguration;
import org.qubership.cloud.core.quarkus.dbaas.datasource.config.flyway.CoreFlywayConfig;
import org.qubership.cloud.core.quarkus.dbaas.datasource.config.properties.DbaaSPostgresDbCreationConfig;
import org.qubership.cloud.core.quarkus.dbaas.datasource.service.MigrationService;
import org.qubership.cloud.core.quarkus.dbaas.datasource.service.impl.DbaaSPostgresDbCreationServiceImpl;
import org.qubership.cloud.dbaas.client.metrics.DbaaSMetricsRegistrar;
import org.qubership.cloud.dbaas.client.service.PostgresqlLogicalDbProvider;
import io.agroal.api.configuration.AgroalConnectionFactoryConfiguration;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.util.TypeLiteral;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class DataSourceCreationServiceImplBuilder {
    private String namespace;
    private MigrationService migrationService;
    private CoreFlywayConfig coreFlywayConfig;
    private AgroalConnectionPoolConfigurationFactory connectionPoolConfigurationFactory;
    private AgroalConnectionFactoryConfiguration connectionFactoryConfiguration;
    private DbaaSPostgresDbCreationConfig postgresDbConfiguration;
    private DbaasDatasourcePoolConfiguration dbaasPoolConfiguration;
    private Instance<PostgresqlLogicalDbProvider> dbProviders;
    private DbaaSMetricsRegistrar metricsRegistrar;


    private DataSourceCreationServiceImplBuilder() {
    }

    public static DataSourceCreationServiceImplBuilder builder() {
        return new DataSourceCreationServiceImplBuilder();
    }

    public DataSourceCreationServiceImplBuilder setNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public DataSourceCreationServiceImplBuilder setMigrationService(MigrationService migrationService) {
        this.migrationService = migrationService;
        return this;
    }

    public DataSourceCreationServiceImplBuilder setCoreFlywayConfig(CoreFlywayConfig coreFlywayConfig) {
        this.coreFlywayConfig = coreFlywayConfig;
        return this;
    }

    public DataSourceCreationServiceImplBuilder setConnectionFactoryConfiguration(AgroalConnectionFactoryConfiguration connectionFactoryConfiguration) {
        this.connectionFactoryConfiguration = connectionFactoryConfiguration;
        return this;
    }

    public DataSourceCreationServiceImplBuilder setConnectionPoolConfigurationFactory(AgroalConnectionPoolConfigurationFactory connectionPoolConfigurationFactory) {
        this.connectionPoolConfigurationFactory = connectionPoolConfigurationFactory;
        return this;
    }

    public DataSourceCreationServiceImplBuilder setPostgresDbConfiguration(DbaaSPostgresDbCreationConfig postgresDbConfiguration) {
        this.postgresDbConfiguration = postgresDbConfiguration;
        return this;
    }

    public DataSourceCreationServiceImplBuilder setDbaasPoolConfiguration(DbaasDatasourcePoolConfiguration dbaasPoolConfiguration) {
        this.dbaasPoolConfiguration = dbaasPoolConfiguration;
        return this;
    }

    public DataSourceCreationServiceImplBuilder setDbProviders(List<PostgresqlLogicalDbProvider> dbProviders) {
        this.dbProviders = new InstanceImpl(dbProviders);
        return this;
    }

    public DataSourceCreationServiceImplBuilder setMetricsRegistrar(DbaaSMetricsRegistrar metricsRegistrar) {
        this.metricsRegistrar = metricsRegistrar;
        return this;
    }

    public DbaaSPostgresDbCreationServiceImpl build() {
        return new DbaaSPostgresDbCreationServiceImpl(namespace, migrationService, coreFlywayConfig,
                connectionPoolConfigurationFactory, connectionFactoryConfiguration,
                postgresDbConfiguration, dbaasPoolConfiguration, dbProviders, metricsRegistrar);
    }

    private static class InstanceImpl implements Instance<PostgresqlLogicalDbProvider> {
        List<PostgresqlLogicalDbProvider> dbProviders;

        public InstanceImpl(List<PostgresqlLogicalDbProvider> dbProviders) {
            this.dbProviders = dbProviders;
        }

        @Override
        public Instance<PostgresqlLogicalDbProvider> select(Annotation... qualifiers) {
            return null;
        }

        @Override
        public <U extends PostgresqlLogicalDbProvider> Instance<U> select(Class<U> subtype, Annotation... qualifiers) {
            return null;
        }

        @Override
        public <U extends PostgresqlLogicalDbProvider> Instance<U> select(TypeLiteral<U> subtype, Annotation... qualifiers) {
            return null;
        }

        @Override
        public Stream<PostgresqlLogicalDbProvider> stream() {
            return Instance.super.stream();
        }

        @Override
        public boolean isUnsatisfied() {
            return false;
        }

        @Override
        public boolean isAmbiguous() {
            return false;
        }

        @Override
        public boolean isResolvable() {
            return Instance.super.isResolvable();
        }

        @Override
        public void destroy(PostgresqlLogicalDbProvider instance) {

        }

        @Override
        public Handle<PostgresqlLogicalDbProvider> getHandle() {
            return null;
        }

        @Override
        public Iterable<? extends Handle<PostgresqlLogicalDbProvider>> handles() {
            return null;
        }

        @Override
        public Stream<? extends Handle<PostgresqlLogicalDbProvider>> handlesStream() {
            return Instance.super.handlesStream();
        }

        @NotNull
        @Override
        public Iterator<PostgresqlLogicalDbProvider> iterator() {
            return dbProviders.iterator();
        }

        @Override
        public void forEach(Consumer<? super PostgresqlLogicalDbProvider> action) {
            Instance.super.forEach(action);
        }

        @Override
        public Spliterator<PostgresqlLogicalDbProvider> spliterator() {
            return Instance.super.spliterator();
        }

        @Override
        public PostgresqlLogicalDbProvider get() {
            return null;
        }
    }
}