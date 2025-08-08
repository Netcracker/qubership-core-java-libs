package com.netcracker.cloud.dbaas.client.cassandra.service;

import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.ProgrammaticDriverConfigLoaderBuilder;
import com.datastax.oss.driver.api.core.session.ProgrammaticArguments;
import org.qubership.cloud.dbaas.client.cassandra.entity.DbaasCassandraMetricsProperties;
import org.qubership.cloud.dbaas.client.cassandra.entity.DbaasCassandraProperties;
import org.qubership.cloud.dbaas.client.cassandra.entity.connection.CassandraDBConnection;
import org.qubership.cloud.security.core.utils.tls.TlsUtils;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class CassandraSessionBuilderTest {

    private static final String EXPECTED_APPLICATION_NAME = "appName2";
    private static final String EXPECTED_CONTACT_POINT = "point1";
    private static final int EXPECTED_PORT = 1234;
    private static final String EXPECTED_KEYSPACE = "space1";
    private static final String EXPECTED_USER_NAME = "user";
    private static final String EXPECTED_PASSWORD = "pass";

    @Test
    public void testCassandraDriverOption_buildSession() {
        try (MockedStatic<TlsUtils> utilities = Mockito.mockStatic(TlsUtils.class)) {
            utilities.when(TlsUtils::getCertificateStorePassword).thenReturn("ttt");
            utilities.when(TlsUtils::getTrustStorePath).thenReturn("aaa");
        }


        CassandraDBConnection connection = new CassandraDBConnection();
        connection.setContactPoints(Arrays.asList(EXPECTED_CONTACT_POINT));
        connection.setPort(EXPECTED_PORT);
        connection.setKeyspace(EXPECTED_KEYSPACE);
        connection.setUsername(EXPECTED_USER_NAME);
        connection.setPassword(EXPECTED_PASSWORD);

        CqlSessionBuilder builder0 = new CqlSessionBuilder() {

            @NonNull
            @Override
            public CqlSessionBuilder withKeyspace(@Nullable String keyspaceName) {
                return this.withKeyspace(CqlIdentifier.fromInternal(keyspaceName));
            }

            @NonNull
            @Override
            public CqlSession build() {
                List<String> contactPoints = this.configLoader.getInitialConfig()
                        .getDefaultProfile().getStringList(DefaultDriverOption.CONTACT_POINTS);
                assertNotNull(contactPoints);
                boolean dbaasContactPointAvailable = contactPoints.stream()
                        .anyMatch(s -> String.format("%s:%s", EXPECTED_CONTACT_POINT, EXPECTED_PORT).equals(s));
                assertTrue(dbaasContactPointAvailable);

                assertEquals(EXPECTED_KEYSPACE, this.keyspace.asInternal());

                ProgrammaticArguments args = this.programmaticArgumentsBuilder.build();
                assertEquals(EXPECTED_APPLICATION_NAME, args.getStartupApplicationName());

                return mock(CqlSession.class);
            }
        };

        DbaasCassandraProperties dbaasCassandraProperties = new DbaasCassandraProperties();
        DbaasCassandraMetricsProperties metricsProperties = new DbaasCassandraMetricsProperties();
        metricsProperties.setEnabled(false);
        dbaasCassandraProperties.setMetrics(metricsProperties);
        dbaasCassandraProperties.setSsl(true);

        DbaasCqlSessionBuilderCustomizer customizer1 = new DbaasCqlSessionBuilderCustomizer() {
            @Override
            public void customize(CqlSessionBuilder cqlSessionBuilder) {
                cqlSessionBuilder.withKeyspace("customizerKeySpace1");
                cqlSessionBuilder.withApplicationName("appName1");
            }

            @Override
            public void customize(ProgrammaticDriverConfigLoaderBuilder configLoader) {

            }
        };

        DbaasCqlSessionBuilderCustomizer customizer2 = new DbaasCqlSessionBuilderCustomizer() {
            @Override
            public void customize(CqlSessionBuilder cqlSessionBuilder) {
                cqlSessionBuilder.withKeyspace("customizerKeySpace2");
                cqlSessionBuilder.withApplicationName(EXPECTED_APPLICATION_NAME);

                cqlSessionBuilder.addContactPoint(InetSocketAddress.createUnresolved("customizerContactPoint", 9876));
            }

            @Override
            public void customize(ProgrammaticDriverConfigLoaderBuilder configLoader) {

            }
        };

        List<DbaasCqlSessionBuilderCustomizer> customizers = Arrays.asList(
                new DefaultDbaasCqlSessionBuilderCustomizer(dbaasCassandraProperties),
                customizer1, customizer2);

        CassandraSessionBuilder builder = new CassandraSessionBuilder(customizers, null) {
            @Override
            public CqlSessionBuilder builder() {
                return builder0;
            }
        };

        builder.build(connection);
    }

    @Test
    public void testEnableInternalTls() {
        try (MockedStatic<TlsUtils> utilities = Mockito.mockStatic(TlsUtils.class)) {
            utilities.when(TlsUtils::isInternalTlsEnabled).thenReturn(true);
            utilities.when(TlsUtils::getCertificateStorePassword).thenReturn("changeit");
            utilities.when(TlsUtils::getTrustStorePath).thenReturn("/test/path");

            CassandraDBConnection connection = new CassandraDBConnection();
            connection.setContactPoints(Arrays.asList(EXPECTED_CONTACT_POINT));
            connection.setPort(EXPECTED_PORT);
            connection.setKeyspace(EXPECTED_KEYSPACE);
            connection.setUsername(EXPECTED_USER_NAME);
            connection.setPassword(EXPECTED_PASSWORD);
            connection.setTls(true);

            DbaasCassandraProperties dbaasCassandraProperties = new DbaasCassandraProperties();
            DbaasCassandraMetricsProperties metricsProperties = new DbaasCassandraMetricsProperties();
            metricsProperties.setEnabled(false);
            dbaasCassandraProperties.setMetrics(metricsProperties);
            dbaasCassandraProperties.setSsl(true);

            CqlSessionBuilder builder0 = new CqlSessionBuilder() {

                @NonNull
                @Override
                public CqlSessionBuilder withKeyspace(@Nullable String keyspaceName) {
                    return this.withKeyspace(CqlIdentifier.fromInternal(keyspaceName));
                }

                @NonNull
                @Override
                public CqlSession build() {
                    List<String> contactPoints = this.configLoader.getInitialConfig()
                            .getDefaultProfile().getStringList(DefaultDriverOption.CONTACT_POINTS);
                    assertNotNull(contactPoints);
                    boolean dbaasContactPointAvailable = contactPoints.stream()
                            .anyMatch(s -> String.format("%s:%s", EXPECTED_CONTACT_POINT, EXPECTED_PORT).equals(s));
                    assertTrue(dbaasContactPointAvailable);

                    return mock(CqlSession.class);
                }
            };

            CassandraSessionBuilder builder = new CassandraSessionBuilder(Collections.emptyList(), null) {
                @Override
                public CqlSessionBuilder builder() {
                    return builder0;
                }
            };

            builder.build(connection);
        }
    }
}
