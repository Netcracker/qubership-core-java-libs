package org.qubership.cloud.quarkus.dbaas.cassandraclient.it;

import org.qubership.cloud.framework.contexts.tenant.context.TenantContext;
import org.qubership.cloud.dbaas.client.cassandra.entity.DbaasCassandraProperties;
import org.qubership.cloud.dbaas.client.cassandra.entity.database.CassandraDatabase;
import org.qubership.cloud.dbaas.client.management.DbaasDbClassifier;
import org.qubership.cloud.dbaas.client.metrics.MetricsProvider;
import org.qubership.cloud.quarkus.dbaas.cassandraclient.config.properties.CassandraProperties;
import org.qubership.cloud.quarkus.dbaas.cassandraclient.service.CassandraClientCreation;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Metrics;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.qubership.cloud.dbaas.client.DbaasConst.SCOPE;
import static org.qubership.cloud.dbaas.client.DbaasConst.SERVICE;
import static org.qubership.cloud.dbaas.client.DbaasConst.TENANT;
import static org.qubership.cloud.dbaas.client.DbaasConst.TENANT_ID;
import static org.qubership.cloud.dbaas.client.metrics.DatabaseMetricProperties.CLASSIFIER_TAG_PREFIX;
import static org.qubership.cloud.quarkus.dbaas.cassandraclient.it.TestContainersCassandraLogicalDbProvider.SERVICE_KEYSPACE;
import static org.qubership.cloud.quarkus.dbaas.cassandraclient.it.TestContainersCassandraLogicalDbProvider.TENANT_KEYSPACE_FORMAT;

@QuarkusTest
@TestProfile(CassandraResourceProfile.class)
public class MetricsTest {

    static String FIRST_TENANT = "a";
    static String SECOND_TENANT = "b";

    @Inject
    CassandraClientCreation cassandraClientCreation;

    @Inject
    CassandraProperties cassandraProperties;

    @Inject
    Instance<MetricsProvider<CassandraDatabase>> metricsProvider;

    @Test
    void checkMetricsProviderExists() {
        Assertions.assertFalse(metricsProvider.isUnsatisfied());
    }

    @Test
    void checkMetricProperties() {
        DbaasCassandraProperties dbaasCassandraProperties = cassandraProperties.getCassandraSessionProperties().getDbaasCassandraProperties();
        Assertions.assertTrue(dbaasCassandraProperties.getMetrics().getEnabled());
        Assertions.assertEquals(List.of("bytes-sent", "bytes-received", "connected-nodes", "cql-requests"), dbaasCassandraProperties.getMetrics().getSession().getEnabled());
        Assertions.assertEquals(List.of("pool.open-connections", "pool.available-streams", "pool.in-flight"), dbaasCassandraProperties.getMetrics().getNode().getEnabled());
        Assertions.assertEquals(Duration.ofSeconds(10), dbaasCassandraProperties.getMetrics().getSession().getCqlRequests().getHighestLatency());
        Assertions.assertEquals(Duration.ofMillis(10), dbaasCassandraProperties.getMetrics().getSession().getCqlRequests().getLowestLatency());
        Assertions.assertEquals(2, dbaasCassandraProperties.getMetrics().getSession().getCqlRequests().getSignificantDigits());

        Assertions.assertEquals(Duration.ofSeconds(10), dbaasCassandraProperties.getMetrics().getSession().getThrottling().getDelay().getHighestLatency());
        Assertions.assertEquals(Duration.ofMillis(10), dbaasCassandraProperties.getMetrics().getSession().getThrottling().getDelay().getLowestLatency());
        Assertions.assertEquals(2, dbaasCassandraProperties.getMetrics().getSession().getThrottling().getDelay().getSignificantDigits());
    }

    @Test
    void checkMetricsRegistration() {
        cassandraClientCreation.getOrCreateCassandraDatabase(getServiceClassifier());
        TenantContext.set(FIRST_TENANT);
        cassandraClientCreation.getOrCreateCassandraDatabase(getTenantClassifier(FIRST_TENANT));
        TenantContext.set(SECOND_TENANT);
        cassandraClientCreation.getOrCreateCassandraDatabase(getTenantClassifier(SECOND_TENANT));

        List<Meter> meters = Metrics.globalRegistry.getMeters();
        Assertions.assertTrue(meters.stream().anyMatch(meter -> meter.getId().getName().equals("cassandra.session.bytes-sent")
                && SERVICE_KEYSPACE.equals(meter.getId().getTag("name"))
                && meter.getId().getTag(CLASSIFIER_TAG_PREFIX + SCOPE).equals(SERVICE)));
        Assertions.assertTrue(meters.stream().anyMatch(meter -> meter.getId().getName().equals("cassandra.session.bytes-received")
                && SERVICE_KEYSPACE.equals(meter.getId().getTag("name"))
                && meter.getId().getTag(CLASSIFIER_TAG_PREFIX + SCOPE).equals(SERVICE)));
        Assertions.assertTrue(meters.stream().anyMatch(meter -> meter.getId().getName().equals("cassandra.session.connected-nodes")
                && SERVICE_KEYSPACE.equals(meter.getId().getTag("name"))
                && meter.getId().getTag(CLASSIFIER_TAG_PREFIX + SCOPE).equals(SERVICE)));
        Assertions.assertTrue(meters.stream().anyMatch(meter -> meter.getId().getName().equals("cassandra.session.cql-requests")
                && SERVICE_KEYSPACE.equals(meter.getId().getTag("name"))
                && meter.getId().getTag(CLASSIFIER_TAG_PREFIX + SCOPE).equals(SERVICE)));

        Assertions.assertTrue(meters.stream().anyMatch(meter -> SERVICE_KEYSPACE.equals(meter.getId().getTag("name"))
                && meter.getId().getTag(CLASSIFIER_TAG_PREFIX + SCOPE).equals(SERVICE)));
        Assertions.assertTrue(meters.stream().anyMatch(meter -> String.format(TENANT_KEYSPACE_FORMAT, FIRST_TENANT).equals(meter.getId().getTag("name"))
                && meter.getId().getTag(CLASSIFIER_TAG_PREFIX + TENANT_ID).equals(FIRST_TENANT)
                && meter.getId().getTag(CLASSIFIER_TAG_PREFIX + SCOPE).equals(TENANT)));
        Assertions.assertTrue(meters.stream().anyMatch(meter -> String.format(TENANT_KEYSPACE_FORMAT, SECOND_TENANT).equals(meter.getId().getTag("name"))
                && meter.getId().getTag(CLASSIFIER_TAG_PREFIX + TENANT_ID).equals(SECOND_TENANT)
                && meter.getId().getTag(CLASSIFIER_TAG_PREFIX + SCOPE).equals(TENANT)));
    }

    private DbaasDbClassifier getServiceClassifier() {
        Map<String, Object> params = new HashMap<>();
        params.put("microserviceName", "test-service");
        params.put("dbClassifier", "default");
        params.put(SCOPE, SERVICE);
        return new DbaasDbClassifier(params);
    }

    private DbaasDbClassifier getTenantClassifier(String tenantId) {
        Map<String, Object> params = new HashMap<>();
        params.put("microserviceName", "test-service");
        params.put("dbClassifier", "default");
        params.put(SCOPE, TENANT);
        params.put(TENANT_ID, tenantId);
        return new DbaasDbClassifier(params);
    }
}
