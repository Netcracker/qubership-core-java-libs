package com.netcracker.cloud.dbaas.client.cassandra.service;

import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.api.core.config.ProgrammaticDriverConfigLoaderBuilder;
import org.qubership.cloud.dbaas.client.cassandra.entity.DbaasCassandraMetricsProperties;
import org.qubership.cloud.dbaas.client.cassandra.entity.DbaasCassandraProperties;
import org.qubership.cloud.dbaas.client.cassandra.entity.connection.CassandraDBConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class DefaultDbaasCqlSessionBuilderCustomizerTest {

    private static final String EXPECTED_DEFAULT_SSL_ENG_FACTORY = "com.datastax.oss.driver.internal.core.ssl.DefaultSslEngineFactory";
    private static final long EXPECTED_REQUEST_TIMEOUT = 180000;

    @BeforeEach
    public void setUp() throws Exception {
    }

    @Test
    public void testCassandraDriverOption_OverridingFromSystemProperty() {
        long expectedValue = 15;
        System.setProperty("datastax-java-driver." + DefaultDriverOption.REQUEST_TIMEOUT.getPath(), String.valueOf(expectedValue));
        CassandraDBConnection connection = new CassandraDBConnection();
        connection.setContactPoints(new ArrayList<>());
        ProgrammaticDriverConfigLoaderBuilder configLoader = DriverConfigLoader.programmaticBuilder();

        DbaasCassandraProperties dbaasCassandraProperties = new DbaasCassandraProperties();
        DbaasCassandraMetricsProperties metricsProperties = new DbaasCassandraMetricsProperties();
        metricsProperties.setEnabled(false);
        dbaasCassandraProperties.setMetrics(metricsProperties);
        DefaultDbaasCqlSessionBuilderCustomizer def = new DefaultDbaasCqlSessionBuilderCustomizer(dbaasCassandraProperties);
        def.customize(configLoader);

        long valueFromConfigLoader = configLoader.build().getInitialConfig()
                .getDefaultProfile().getDuration(DefaultDriverOption.REQUEST_TIMEOUT).toMillis();
        assertEquals(expectedValue, valueFromConfigLoader);

        System.getProperties().remove("datastax-java-driver." + DefaultDriverOption.REQUEST_TIMEOUT.getPath());
    }

    @Test
    public void testCassandraDriverOption_EnablingSsl() {
        ProgrammaticDriverConfigLoaderBuilder configLoaderBuilder = DriverConfigLoader.programmaticBuilder();

        DbaasCassandraProperties properties = new DbaasCassandraProperties();
        properties.setSsl(true);
        properties.setSslHostnameValidation(true);
        properties.setLbSlowReplicaAvoidance(false);
        DbaasCassandraMetricsProperties metricsProperties = new DbaasCassandraMetricsProperties();
        metricsProperties.setEnabled(false);
        properties.setMetrics(metricsProperties);


        DefaultDbaasCqlSessionBuilderCustomizer def = new DefaultDbaasCqlSessionBuilderCustomizer(properties);
        def.customize(configLoaderBuilder);

        DriverConfigLoader configLoader = configLoaderBuilder.build();
        String factoryClass = configLoader.getInitialConfig()
                .getDefaultProfile().getString(DefaultDriverOption.SSL_ENGINE_FACTORY_CLASS);
        assertEquals(EXPECTED_DEFAULT_SSL_ENG_FACTORY, factoryClass);

        long valueFromConfigLoader = configLoader.getInitialConfig()
                .getDefaultProfile().getDuration(DefaultDriverOption.REQUEST_TIMEOUT).toMillis();
        assertEquals(EXPECTED_REQUEST_TIMEOUT, valueFromConfigLoader);

        final boolean isSslHostnameValidation = configLoader.getInitialConfig()
                .getDefaultProfile().getBoolean(DefaultDriverOption.SSL_HOSTNAME_VALIDATION);
        assertTrue(isSslHostnameValidation);

        final boolean isLbSlowReplicaAvoidance = configLoader.getInitialConfig()
                .getDefaultProfile().getBoolean(DefaultDriverOption.LOAD_BALANCING_POLICY_SLOW_AVOIDANCE);
        assertFalse(isLbSlowReplicaAvoidance);
    }
}
