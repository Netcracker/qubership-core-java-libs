package org.qubership.cloud.quarkus.dbaas.cassandraclient.config;

import com.datastax.oss.driver.api.core.CqlSession;
import org.qubership.cloud.dbaas.client.cassandra.entity.database.CassandraDatabase;
import org.qubership.cloud.dbaas.client.cassandra.metrics.CassandraMetricsProvider;
import org.qubership.cloud.dbaas.client.cassandra.migration.MigrationExecutor;
import org.qubership.cloud.dbaas.client.cassandra.migration.MigrationExecutorImpl;
import org.qubership.cloud.dbaas.client.cassandra.migration.model.settings.SchemaMigrationSettings;
import org.qubership.cloud.dbaas.client.cassandra.migration.service.SchemaVersionResourceReader;
import org.qubership.cloud.dbaas.client.cassandra.migration.service.SchemaVersionResourceReaderImpl;
import org.qubership.cloud.dbaas.client.cassandra.migration.service.extension.AlreadyMigratedVersionsExtensionPoint;
import org.qubership.cloud.dbaas.client.cassandra.migration.service.resource.SchemaVersionResourceFinderRegistry;
import org.qubership.cloud.dbaas.client.cassandra.service.CassandraSessionBuilder;
import org.qubership.cloud.dbaas.client.cassandra.service.DbaasCqlSessionBuilderCustomizer;
import org.qubership.cloud.dbaas.client.cassandra.service.DefaultDbaasCqlSessionBuilderCustomizer;
import org.qubership.cloud.dbaas.client.metrics.DbaaSMetricsRegistrar;
import org.qubership.cloud.dbaas.client.metrics.MetricsProvider;
import org.qubership.cloud.dbaas.common.postprocessor.PostConnectProcessorManager;
import org.qubership.cloud.dbaas.common.postprocessor.QuarkusPostConnectProcessor;
import org.qubership.cloud.quarkus.dbaas.cassandraclient.DbaaSCassandraClient;
import org.qubership.cloud.quarkus.dbaas.cassandraclient.classifier.ServiceClassifierBuilder;
import org.qubership.cloud.quarkus.dbaas.cassandraclient.classifier.TenantClassifierBuilder;
import org.qubership.cloud.quarkus.dbaas.cassandraclient.config.properties.CassandraProperties;
import org.qubership.cloud.quarkus.dbaas.cassandraclient.service.CassandraClientCreation;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.properties.UnlessBuildProperty;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class CassandraClientConfiguration {
    public static final String SERVICE_CASSANDRA_CLIENT = "serviceCassandraClient";
    public static final String TENANT_CASSANDRA_CLIENT = "tenantCassandraClient";

    @ConfigProperty(name = "cloud.microservice.name")
    String microserviceName;

    @ConfigProperty(name = "cloud.microservice.namespace")
    String namespace;

    @Inject
    CassandraProperties cassandraProperties;

    @Produces
    @DefaultBean
    public PostConnectProcessorManager<CassandraDatabase> getCasandraPostConnectProcessorManager(
            Instance<QuarkusPostConnectProcessor<CassandraDatabase>> postProcessors) {
        return new PostConnectProcessorManager<>(postProcessors);
    }

    @Produces
    public DbaasCqlSessionBuilderCustomizer getDbaasCqlSessionBuilderCustomizer() {
        return new DefaultDbaasCqlSessionBuilderCustomizer(
                cassandraProperties.getCassandraSessionProperties()
                        .getDbaasCassandraProperties());
    }

    @Produces
    @Named(SERVICE_CASSANDRA_CLIENT)
    @DefaultBean
    public CqlSession getDbaaSServiceCassandraClient(CassandraClientCreation cassandraClientCreation) {
        return new DbaaSCassandraClient(new ServiceClassifierBuilder(getInitialClassifierMap()), cassandraClientCreation);
    }

    @Produces
    @Named(TENANT_CASSANDRA_CLIENT)
    @DefaultBean
    public CqlSession getDbaaSTenantCassandraClient(CassandraClientCreation cassandraClientCreation) {
        return new DbaaSCassandraClient(new TenantClassifierBuilder(getInitialClassifierMap()), cassandraClientCreation);
    }

    @Produces
    @DefaultBean
    @ApplicationScoped
    public CassandraSessionBuilder cassandraSessionBuilder(Instance<DbaasCqlSessionBuilderCustomizer> cqlSessionBuilderCustomizers,
                                                           DbaaSMetricsRegistrar dbaaSMetricsRegistrar) {
        return new CassandraSessionBuilder(getSortedCustomizers(cqlSessionBuilderCustomizers), dbaaSMetricsRegistrar);
    }

    @Produces
    @ApplicationScoped
    @UnlessBuildProperty(name = "quarkus.dbaas.cassandra.metrics.enabled", stringValue = "false", enableIfMissing = true)
    public MetricsProvider<CassandraDatabase> cassandraMetricsProvider() {
        return new CassandraMetricsProvider();
    }

    @Produces
    @ApplicationScoped
    @UnlessBuildProperty(name = "quarkus.dbaas.cassandra.migration.enabled", stringValue = "false", enableIfMissing = true)
    public MigrationExecutor cassandraMigrationExecutor(Instance<AlreadyMigratedVersionsExtensionPoint> alreadyMigratedVersionsExtensionPoint) {
        SchemaMigrationSettings schemaMigrationSettings = cassandraProperties.getCassandraSessionProperties().getMigration().toSchemaMigrationSettings();
        SchemaVersionResourceReader schemaVersionResourceReader = new SchemaVersionResourceReaderImpl(schemaMigrationSettings.version(), new SchemaVersionResourceFinderRegistry());
        return new MigrationExecutorImpl(
                schemaMigrationSettings,
                schemaVersionResourceReader,
                alreadyMigratedVersionsExtensionPoint.isResolvable() ? alreadyMigratedVersionsExtensionPoint.get() : null
        );
    }

    private List<DbaasCqlSessionBuilderCustomizer> getSortedCustomizers(Instance<DbaasCqlSessionBuilderCustomizer> customizers) {
        return customizers.stream()
                .sorted(Comparator.comparingInt(o -> getPriority(o.getClass())))
                .collect(Collectors.toList());
    }

    /**
     * Returns priority value obtained from annotation jakarta.annotation.Priority
     * Zero is the highest priority.
     * In case absent of annotation the priority is set to java.lang.Integer#MAX_VALUE
     **/
    private int getPriority(Class<?> clazz) {
        Priority priority = clazz.getAnnotation(Priority.class);
        return priority == null ? Integer.MAX_VALUE : priority.value();
    }

    private Map<String, Object> getInitialClassifierMap() {
        Map<String, Object> params = new HashMap<>();
        params.put("microserviceName", microserviceName);
        params.put("namespace", namespace);
        params.put("dbClassifier", cassandraProperties.getCassandraDbCreationConfig().getDbClassifier());
        return params;
    }
}
