package org.qubership.cloud.dbaas.client.cassandra.service;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.auth.AuthProvider;
import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.api.core.config.ProgrammaticDriverConfigLoaderBuilder;
import com.datastax.oss.driver.internal.core.ssl.DefaultSslEngineFactory;
import com.datastax.oss.driver.shaded.guava.common.collect.Lists;
import org.qubership.cloud.dbaas.client.cassandra.auth.DbaaSAuthProvider;
import org.qubership.cloud.dbaas.client.cassandra.entity.connection.CassandraDBConnection;
import org.qubership.cloud.dbaas.client.cassandra.entity.database.CassandraDatabase;
import org.qubership.cloud.dbaas.client.cassandra.entity.database.type.CassandraDBType;
import org.qubership.cloud.dbaas.client.metrics.DatabaseMetricProperties;
import org.qubership.cloud.dbaas.client.metrics.DbaaSMetricsRegistrar;
import jakarta.inject.Provider;
import lombok.extern.slf4j.Slf4j;
import org.qubership.cloud.security.core.utils.tls.TlsUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class CassandraSessionBuilder {

    private List<DbaasCqlSessionBuilderCustomizer> cqlSessionBuilderCustomizers;
    private DbaaSMetricsRegistrar metricsRegistrar;

    public CassandraSessionBuilder(List<DbaasCqlSessionBuilderCustomizer> cqlSessionBuilderCustomizers,
                                   DbaaSMetricsRegistrar metricsRegistrar) {
        this.cqlSessionBuilderCustomizers = cqlSessionBuilderCustomizers;
        this.metricsRegistrar = metricsRegistrar;
    }

    public CqlSession build(CassandraDatabase database) {
        CassandraDBConnection connection = database.getConnectionProperties();
        CqlSessionBuilder builder = prepareBuilder(connection);

        ProgrammaticDriverConfigLoaderBuilder configLoaderBuilder = prepareConfigLoader(connection, builder);
        registerMetrics(database, configLoaderBuilder);
        CqlSession cqlSession = builder.withConfigLoader(configLoaderBuilder.build()).build();
        return cqlSession;
    }

    /**
     * @deprecated
     * This method doesn't support proper metric registration.
     * <p> Use {@link CassandraSessionBuilder#build(CassandraDatabase)} instead.
     */
    @Deprecated(forRemoval = true)
    public CqlSession build(CassandraDBConnection conn) {
        CqlSessionBuilder sessionBuilder = prepareBuilder(conn);
        return sessionBuilder.withConfigLoader(prepareConfigLoader(conn, sessionBuilder).build()).build();
    }

    public CqlSessionBuilder prepareBuilder(CassandraDBConnection conn) {
        CqlSessionBuilder sessionBuilder = builder();
        cqlSessionBuilderCustomizers.forEach(dbaasCqlSessionBuilderCustomizer
                -> dbaasCqlSessionBuilderCustomizer.customize(sessionBuilder));

        return sessionBuilder.withAuthProvider(getAuthProvider(conn)).withKeyspace(conn.getKeyspace());
    }

    private ProgrammaticDriverConfigLoaderBuilder prepareConfigLoader(CassandraDBConnection conn, CqlSessionBuilder sessionBuilder) {
        ProgrammaticDriverConfigLoaderBuilder configLoader = getConfigLoader();
        cqlSessionBuilderCustomizers.forEach(dbaasCqlSessionBuilderCustomizer
                -> dbaasCqlSessionBuilderCustomizer.customize(configLoader));

        int port = conn.getPort();
        ArrayList<String> contactPointList = Lists.newArrayList();
        for (String contactPoint : conn.getContactPoints()) {
            contactPointList.add(contactPoint + ":" + port);
        }
        configLoader.withStringList(DefaultDriverOption.CONTACT_POINTS, contactPointList);

        if (conn.isTls()) {
            log.info("Connection to cassandra will be secured");
            configLoader.withClass(DefaultDriverOption.SSL_ENGINE_FACTORY_CLASS, DefaultSslEngineFactory.class);
            //configLoader.withBoolean(DefaultDriverOption.SSL_HOSTNAME_VALIDATION, true);
            sessionBuilder.withSslContext(TlsUtils.getSslContext());
        }
        return configLoader;
    }

    private AuthProvider getAuthProvider(CassandraDBConnection conn) {
        return new DbaaSAuthProvider(conn.getUsername(), conn.getPassword());
    }

    private ProgrammaticDriverConfigLoaderBuilder getConfigLoader() {
        return DriverConfigLoader.programmaticBuilder();
    }

    public CqlSessionBuilder builder() {
        return CqlSession.builder();
    }

    private void registerMetrics(CassandraDatabase database, ProgrammaticDriverConfigLoaderBuilder configLoader) {
        if (metricsRegistrar != null) {
            if (database.getName() == null) {
                log.warn("Database name is null");
            }
            DatabaseMetricProperties metricProperties = DatabaseMetricProperties.builder()
                    .databaseName(database.getName())
                    .role(database.getConnectionProperties().getRole())
                    .classifier(database.getClassifier())
                    .extraParameters(Map.of("config_loader", configLoader))
                    .build();
            metricsRegistrar.registerMetrics(CassandraDBType.INSTANCE, metricProperties);
        }
    }

}
