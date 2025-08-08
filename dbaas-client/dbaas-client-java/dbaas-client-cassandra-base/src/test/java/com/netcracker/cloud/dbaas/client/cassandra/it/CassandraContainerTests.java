package org.qubership.cloud.dbaas.client.cassandra.it;

import org.qubership.cloud.dbaas.client.cassandra.entity.DbaasCassandraMetricsProperties;
import org.qubership.cloud.dbaas.client.cassandra.entity.DbaasCassandraProperties;
import org.qubership.cloud.dbaas.client.cassandra.entity.connection.CassandraDBConnection;
import org.qubership.cloud.dbaas.client.cassandra.entity.database.CassandraDatabase;
import org.qubership.cloud.dbaas.client.cassandra.metrics.CassandraMetricsProvider;
import org.qubership.cloud.dbaas.client.cassandra.service.CassandraSessionBuilder;
import org.qubership.cloud.dbaas.client.cassandra.service.DefaultDbaasCqlSessionBuilderCustomizer;
import org.qubership.cloud.dbaas.client.metrics.DbaaSMetricsRegistrar;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Metrics;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.qubership.cloud.dbaas.client.DbaasConst.SCOPE;
import static org.qubership.cloud.dbaas.client.DbaasConst.SERVICE;
import static org.qubership.cloud.dbaas.client.DbaasConst.TENANT;
import static org.qubership.cloud.dbaas.client.DbaasConst.TENANT_ID;
import static org.qubership.cloud.dbaas.client.metrics.DatabaseMetricProperties.CLASSIFIER_TAG_PREFIX;

class CassandraContainerTests {

    static final String TEST_ROLE = "test-role";
    static final String SERVICE_KEYSPACE = "service_db";
    static final String TENANT_KEYSPACE_A = "tenant_db_a";
    static final String TENANT_KEYSPACE_B = "tenant_db_b";

    @BeforeAll
    static void setUp() {
        CassandraTestContainer.getInstance().start();
    }

    @AfterAll
    static void tearDown() {
        if (CassandraTestContainer.getInstance().isRunning()) {
            CassandraTestContainer.getInstance().stop();
        }
    }

    @Test
    void testDbaasCassandraMetrics() {
        DbaaSMetricsRegistrar dbaaSMetricsRegistrar = new DbaaSMetricsRegistrar(Collections.singletonList(new CassandraMetricsProvider()));

        DbaasCassandraProperties dbaasCassandraProperties = new DbaasCassandraProperties();
        DbaasCassandraMetricsProperties metrics = new DbaasCassandraMetricsProperties();
        metrics.setEnabled(true);
        metrics.getSession().setEnabled(List.of("bytes-sent", "bytes-received", "connected-nodes"));
        metrics.getNode().setEnabled(List.of("pool.open-connections", "bytes-sent", "bytes-received"));
        dbaasCassandraProperties.setMetrics(metrics);
        DefaultDbaasCqlSessionBuilderCustomizer customizer = new DefaultDbaasCqlSessionBuilderCustomizer(dbaasCassandraProperties);

        CassandraSessionBuilder cassandraSessionBuilder = new CassandraSessionBuilder(Collections.singletonList(customizer), dbaaSMetricsRegistrar);

        SortedMap<String, Object> serviceClassifier = new TreeMap<>();
        serviceClassifier.put(SCOPE, SERVICE);
        cassandraSessionBuilder.build(getCassandraDatabase(serviceClassifier, SERVICE_KEYSPACE));

        SortedMap<String, Object> tenantClassifierA = new TreeMap<>();
        tenantClassifierA.put(SCOPE, TENANT);
        String tenantA = "tenant_a";
        tenantClassifierA.put(TENANT_ID, tenantA);
        cassandraSessionBuilder.build(getCassandraDatabase(tenantClassifierA, TENANT_KEYSPACE_A));

        SortedMap<String, Object> tenantClassifierB = new TreeMap<>();
        tenantClassifierB.put(SCOPE, TENANT);
        String tenantB = "tenant_b";
        tenantClassifierB.put(TENANT_ID, tenantB);
        cassandraSessionBuilder.build(getCassandraDatabase(tenantClassifierB, TENANT_KEYSPACE_B));

        List<Meter> meters = Metrics.globalRegistry.getMeters();
        Assertions.assertTrue(meters.stream().anyMatch(meter -> meter.getId().getTag("name").equals(SERVICE_KEYSPACE)
                && meter.getId().getTag(CLASSIFIER_TAG_PREFIX + SCOPE).equals(SERVICE)));
        Assertions.assertTrue(meters.stream().anyMatch(meter -> meter.getId().getTag("name").equals(TENANT_KEYSPACE_A)
                && meter.getId().getTag(CLASSIFIER_TAG_PREFIX + TENANT_ID).equals(tenantA)
                && meter.getId().getTag(CLASSIFIER_TAG_PREFIX + SCOPE).equals(TENANT)));
        Assertions.assertTrue(meters.stream().anyMatch(meter -> meter.getId().getTag("name").equals(TENANT_KEYSPACE_B)
                && meter.getId().getTag(CLASSIFIER_TAG_PREFIX + TENANT_ID).equals(tenantB)
                && meter.getId().getTag(CLASSIFIER_TAG_PREFIX + SCOPE).equals(TENANT)));

    }

    static CassandraDatabase getCassandraDatabase(SortedMap<String, Object> classifier, String keyspace) {
        CassandraDBConnection cassandraDBConnection = new CassandraDBConnection();
        InetSocketAddress contactPoint = CassandraTestContainer.getInstance().getContactPoint();
        cassandraDBConnection.setContactPoints(List.of(contactPoint.getHostString()));
        cassandraDBConnection.setPort(contactPoint.getPort());
        cassandraDBConnection.setUsername(CassandraTestContainer.getInstance().getUsername());
        cassandraDBConnection.setPassword(CassandraTestContainer.getInstance().getPassword());

        cassandraDBConnection.setRole(TEST_ROLE);

        cassandraDBConnection.setKeyspace(keyspace);

        CassandraDatabase cassandraDatabase = new CassandraDatabase();
        cassandraDatabase.setName(keyspace);
        cassandraDatabase.setConnectionProperties(cassandraDBConnection);
        cassandraDatabase.setClassifier(classifier);
        return cassandraDatabase;
    }
}
