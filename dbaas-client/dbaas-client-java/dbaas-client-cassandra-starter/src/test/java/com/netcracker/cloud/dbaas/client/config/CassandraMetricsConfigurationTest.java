package com.netcracker.cloud.dbaas.client.config;

import com.netcracker.cloud.dbaas.client.cassandra.entity.DbaasCassandraProperties;
import com.netcracker.cloud.dbaas.client.cassandra.entity.database.type.CassandraDBType;
import com.netcracker.cloud.dbaas.client.cassandra.metrics.CassandraMetricsProvider;
import com.netcracker.cloud.dbaas.client.config.container.CassandraContainerIntegrationConfiguration;
import com.netcracker.cloud.dbaas.client.config.container.CassandraTestContainerConfiguration;
import com.netcracker.cloud.dbaas.client.management.DatabasePool;
import com.netcracker.cloud.dbaas.client.management.DbaasDbClassifier;
import com.netcracker.cloud.dbaas.client.management.classifier.ServiceDbaaSClassifierBuilder;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Duration;
import java.util.List;

import static com.netcracker.cloud.dbaas.client.DbaasConst.SCOPE;
import static com.netcracker.cloud.dbaas.client.DbaasConst.SERVICE;
import static com.netcracker.cloud.dbaas.client.config.container.CassandraContainerIntegrationConfiguration.SERVICE_KEYSPACE;
import static com.netcracker.cloud.dbaas.client.metrics.DatabaseMetricProperties.CLASSIFIER_TAG_PREFIX;

@EnableAutoConfiguration(exclude = CassandraAutoConfiguration.class)
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {CassandraContainerIntegrationConfiguration.class, CassandraTestContainerConfiguration.class},
        properties = {
                "cloud.microservice.name=test-app",
                "cloud.microservice.namespace=default",
                "dbaas.cassandra.metrics.enabled=true",
                "dbaas.cassandra.metrics.session.enabled=bytes-sent,bytes-received,connected-nodes,cql-requests",
                "dbaas.cassandra.metrics.session.cql-requests.highest-latency = 10s",
                "dbaas.cassandra.metrics.session.cql-requests.lowest-latency = 10ms",
                "dbaas.cassandra.metrics.session.cql-requests.significant-digits = 2",
                "dbaas.cassandra.metrics.session.cql-requests.refresh-interval = 1m"
        })
@ActiveProfiles("metrics")
class CassandraMetricsConfigurationTest {

    @Autowired
    DbaasCassandraProperties dbaasCassandraProperties;

    @Autowired
    DatabasePool databasePool;

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    MeterRegistry meterRegistry;


    @Test
    void checkMetricProperties() {
        Assertions.assertTrue(dbaasCassandraProperties.getMetrics().getEnabled());
        Assertions.assertEquals(List.of("bytes-sent", "bytes-received", "connected-nodes", "cql-requests"), dbaasCassandraProperties.getMetrics().getSession().getEnabled());
        Assertions.assertEquals(Duration.ofSeconds(10), dbaasCassandraProperties.getMetrics().getSession().getCqlRequests().getHighestLatency());
        Assertions.assertEquals(Duration.ofMillis(10), dbaasCassandraProperties.getMetrics().getSession().getCqlRequests().getLowestLatency());
        Assertions.assertEquals(2, dbaasCassandraProperties.getMetrics().getSession().getCqlRequests().getSignificantDigits());
        Assertions.assertEquals(Duration.ofMinutes(1), dbaasCassandraProperties.getMetrics().getSession().getCqlRequests().getRefreshInterval());
    }

    @Test
    void checkBeanRegistration() {
        Assertions.assertNotNull(applicationContext.getBean(CassandraMetricsProvider.class));
    }

    @Test
    void checkMetricsRegistration() {
        DbaasDbClassifier classifier = new ServiceDbaaSClassifierBuilder(null).build();
        databasePool.getOrCreateDatabase(CassandraDBType.INSTANCE, classifier);
        List<Meter> meters = meterRegistry.getMeters();
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
    }
}

