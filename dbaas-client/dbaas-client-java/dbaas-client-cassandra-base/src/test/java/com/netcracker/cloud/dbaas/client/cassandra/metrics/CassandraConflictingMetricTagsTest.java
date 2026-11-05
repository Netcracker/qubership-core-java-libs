package com.netcracker.cloud.dbaas.client.cassandra.metrics;

import com.datastax.oss.driver.api.core.AllNodesFailedException;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.metrics.DefaultSessionMetric;
import com.datastax.oss.driver.internal.core.context.DefaultDriverContext;
import com.datastax.oss.driver.internal.core.metadata.DefaultEndPoint;
import com.datastax.oss.driver.internal.core.metadata.DefaultNode;
import com.datastax.oss.driver.internal.core.metrics.SessionMetricUpdater;
import com.netcracker.cloud.dbaas.client.cassandra.entity.DbaasCassandraProperties;
import com.netcracker.cloud.dbaas.client.cassandra.entity.connection.CassandraDBConnection;
import com.netcracker.cloud.dbaas.client.cassandra.entity.database.CassandraDatabase;
import com.netcracker.cloud.dbaas.client.cassandra.service.CassandraSessionBuilder;
import com.netcracker.cloud.dbaas.client.cassandra.service.DbaasCqlSessionBuilderCustomizer;
import com.netcracker.cloud.dbaas.client.cassandra.service.DefaultDbaasCqlSessionBuilderCustomizer;
import com.netcracker.cloud.dbaas.client.metrics.DbaaSMetricsRegistrar;
import com.netcracker.cloud.dbaas.client.metrics.DbaasMetricTagsFilter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.netcracker.cloud.dbaas.client.DbaasConst.SCOPE;
import static com.netcracker.cloud.dbaas.client.DbaasConst.SERVICE;
import static com.netcracker.cloud.dbaas.client.DbaasConst.TENANT;
import static com.netcracker.cloud.dbaas.client.DbaasConst.TENANT_ID;
import static com.netcracker.cloud.dbaas.client.metrics.DatabaseMetricProperties.CLASSIFIER_TAG_PREFIX;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.Mockito.mock;

/**
 * Uses the registry that Quarkus creates in PrometheusMeterRegistryProducer: the simpleclient one with
 * throwExceptionOnRegistrationFailure. The newer io.micrometer.prometheusmetrics registry accepts different tag keys since 1.17.
 */
class CassandraConflictingMetricTagsTest {

    private static final List<String> SESSION_METRICS = List.of("bytes-sent", "connected-nodes", "cql-requests");
    private static final List<String> NODE_METRICS = List.of("pool.open-connections", "bytes-sent", "cql-messages");
    private static final DefaultEndPoint END_POINT = new DefaultEndPoint(new InetSocketAddress("127.0.0.1", 9042));
    private static final String BYTES_SENT = "cassandra.session.bytes-sent";
    private static final String CONNECTED_NODES = "cassandra.session.connected-nodes";
    private static final String CQL_REQUESTS = "cassandra.session.cql-requests";
    private static final String CL_SCOPE = CLASSIFIER_TAG_PREFIX + SCOPE;
    private static final String CL_TENANT_ID = CLASSIFIER_TAG_PREFIX + TENANT_ID;

    private final List<DefaultDriverContext> contexts = new ArrayList<>();
    private final List<MeterRegistry> registries = new ArrayList<>();
    private PrometheusMeterRegistry prometheus;
    private int port;

    @BeforeEach
    void setUp() throws IOException {
        Metrics.globalRegistry.clear();
        prometheus = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        prometheus.throwExceptionOnRegistrationFailure();
        // On a service the filter is added when the registry already has meters
        prometheus.counter("registered.before.filter");
        addRegistry(prometheus);
        port = freePort();
    }

    @AfterEach
    void tearDown() {
        contexts.forEach(context -> context.getNettyOptions().onClose());
        registries.forEach(Metrics::removeRegistry);
        Metrics.globalRegistry.clear();
        registries.forEach(MeterRegistry::close);
    }

    @Test
    void conflictingSessionMetricsDoNotFailSession() {
        List<DbaasCqlSessionBuilderCustomizer> customizers = customizers(SESSION_METRICS, List.of());

        registerMetricsOnly(customizers, serviceDatabase());

        assertReachesConnection(customizers, tenantDatabase("tenant_a"));
    }

    @Test
    void conflictingNodeMetricsDoNotFailSession() {
        List<DbaasCqlSessionBuilderCustomizer> customizers = customizers(List.of(), NODE_METRICS);

        registerMetricsOnly(customizers, serviceDatabase());

        assertReachesConnection(customizers, tenantDatabase("tenant_a"));
    }

    @Test
    void firstKindOfDatabaseKeepsPrometheusMetrics() {
        List<DbaasCqlSessionBuilderCustomizer> customizers = customizers(SESSION_METRICS, NODE_METRICS);

        registerMetricsOnly(customizers, serviceDatabase());
        registerMetricsOnly(customizers, tenantDatabase("tenant_a"));

        assertNotNull(prometheus.find(CQL_REQUESTS).tag(CL_SCOPE, SERVICE).timer());
        assertNull(prometheus.find(CQL_REQUESTS).tag(CL_SCOPE, TENANT).timer());
    }

    @Test
    void sameTagKeysKeepPrometheusMetricsOfBothDatabases() {
        List<DbaasCqlSessionBuilderCustomizer> customizers = customizers(SESSION_METRICS, NODE_METRICS);

        registerMetricsOnly(customizers, tenantDatabase("tenant_a"));
        registerMetricsOnly(customizers, tenantDatabase("tenant_b"));

        assertNotNull(prometheus.find(CQL_REQUESTS).tag(CL_TENANT_ID, "tenant_a").timer());
        assertNotNull(prometheus.find(CQL_REQUESTS).tag(CL_TENANT_ID, "tenant_b").timer());
    }

    @Test
    void onlyConflictingMetricIsLostInPrometheus() {
        registerMetricsOnly(customizers(List.of("connected-nodes"), List.of()), serviceDatabase());
        registerMetricsOnly(customizers(List.of("connected-nodes", "cql-requests"), List.of()), tenantDatabase("tenant_a"));

        assertNotNull(prometheus.find(CONNECTED_NODES).tag(CL_SCOPE, SERVICE).gauge());
        assertNull(prometheus.find(CONNECTED_NODES).tag(CL_SCOPE, TENANT).gauge());
        assertNotNull(prometheus.find(CQL_REQUESTS).tag(CL_SCOPE, TENANT).timer());
    }

    // The driver registers bytes-sent on its first update, which happens during a request
    @Test
    void metricRegisteredDuringRequestDoesNotFailRequest() {
        List<DbaasCqlSessionBuilderCustomizer> customizers = customizers(List.of("bytes-sent"), List.of());

        registerMetricsOnly(customizers, serviceDatabase());
        registerMetricsOnly(customizers, tenantDatabase("tenant_a"));
        sessionUpdater(0).incrementCounter(DefaultSessionMetric.BYTES_SENT, null, 1);

        assertDoesNotThrow(() -> sessionUpdater(1).incrementCounter(DefaultSessionMetric.BYTES_SENT, null, 1));
        assertNotNull(prometheus.find(BYTES_SENT).tag(CL_SCOPE, SERVICE).counter());
        assertNull(prometheus.find(BYTES_SENT).tag(CL_SCOPE, TENANT).counter());
    }

    @Test
    void otherRegistriesGetMetricsOfBothDatabases() {
        List<MeterRegistry> others = Stream.<MeterRegistry>generate(SimpleMeterRegistry::new).limit(5).map(this::addRegistry).toList();
        List<DbaasCqlSessionBuilderCustomizer> customizers = customizers(SESSION_METRICS, List.of());

        registerMetricsOnly(customizers, serviceDatabase());
        registerMetricsOnly(customizers, tenantDatabase("tenant_a"));
        sessionUpdater(1).updateTimer(DefaultSessionMetric.CQL_REQUESTS, null, 5, TimeUnit.MILLISECONDS);

        for (MeterRegistry registry : others) {
            assertNotNull(registry.find(CQL_REQUESTS).tag(CL_SCOPE, SERVICE).timer());
            assertEquals(1, registry.find(CQL_REQUESTS).tag(CL_SCOPE, TENANT).timer().count());
        }
        assertNull(prometheus.find(CQL_REQUESTS).tag(CL_SCOPE, TENANT).timer());
    }

    @Test
    void tagKeysAreFreeAfterMetersAreRemoved() {
        List<DbaasCqlSessionBuilderCustomizer> customizers = customizers(SESSION_METRICS, List.of());

        registerMetricsOnly(customizers, serviceDatabase());
        sessionUpdater(0).clearMetrics();
        registerMetricsOnly(customizers, tenantDatabase("tenant_a"));

        assertNotNull(prometheus.find(CQL_REQUESTS).tag(CL_SCOPE, TENANT).timer());
    }

    @Test
    void registrationDuringRequestDoesNotFailWhenTagKeysWereTaken() {
        List<DbaasCqlSessionBuilderCustomizer> customizers = customizers(SESSION_METRICS, List.of());

        registerMetricsOnly(customizers, serviceDatabase());
        sessionUpdater(0).clearMetrics();
        registerMetricsOnly(customizers, tenantDatabase("tenant_a"));

        assertDoesNotThrow(() -> sessionUpdater(0).updateTimer(DefaultSessionMetric.CQL_REQUESTS, null, 5, TimeUnit.MILLISECONDS));
        assertNull(prometheus.find(CQL_REQUESTS).tag(CL_SCOPE, SERVICE).timer());
    }

    // Nothing listens on the port, so the connection error shows that the session got past the metrics.
    // The timeout is needed because a failure on node metrics leaves the session initialization hanging forever.
    private void assertReachesConnection(List<DbaasCqlSessionBuilderCustomizer> customizers, CassandraDatabase database) {
        CassandraSessionBuilder sessionBuilder = new CassandraSessionBuilder(customizers, registrar());
        assertTimeoutPreemptively(Duration.ofSeconds(60),
                () -> assertThrows(AllNodesFailedException.class, () -> sessionBuilder.build(database)));
    }

    // A session that fails to connect removes its meters, so this keeps them registered as an open session does
    private void registerMetricsOnly(List<DbaasCqlSessionBuilderCustomizer> customizers, CassandraDatabase database) {
        new CassandraSessionBuilder(customizers, registrar()) {
            @Override
            public CqlSessionBuilder builder() {
                return new CqlSessionBuilder() {
                    @NonNull
                    @Override
                    public CqlSession build() {
                        DefaultDriverContext context = new DefaultDriverContext(configLoader, programmaticArgumentsBuilder.build());
                        contexts.add(context);
                        context.getMetricsFactory();
                        new DefaultNode(END_POINT, context);
                        return mock(CqlSession.class);
                    }
                };
            }
        }.build(database);
    }

    private SessionMetricUpdater sessionUpdater(int contextIndex) {
        return contexts.get(contextIndex).getMetricsFactory().getSessionUpdater();
    }

    private <T extends MeterRegistry> T addRegistry(T registry) {
        registries.add(registry);
        DbaasMetricTagsFilter.applyTo(registry);
        Metrics.addRegistry(registry);
        return registry;
    }

    private static List<DbaasCqlSessionBuilderCustomizer> customizers(List<String> sessionMetrics, List<String> nodeMetrics) {
        DbaasCassandraProperties properties = new DbaasCassandraProperties();
        properties.getMetrics().getSession().setEnabled(sessionMetrics);
        properties.getMetrics().getNode().setEnabled(nodeMetrics);
        return List.of(new DefaultDbaasCqlSessionBuilderCustomizer(properties));
    }

    private static DbaaSMetricsRegistrar registrar() {
        return new DbaaSMetricsRegistrar(List.of(new CassandraMetricsProvider()));
    }

    private CassandraDatabase serviceDatabase() {
        return database("service_db", SERVICE, null);
    }

    private CassandraDatabase tenantDatabase(String tenantId) {
        return database("db_" + tenantId, TENANT, tenantId);
    }

    private CassandraDatabase database(String name, String scope, String tenantId) {
        CassandraDBConnection connection = new CassandraDBConnection();
        connection.setContactPoints(List.of("127.0.0.1"));
        connection.setPort(port);
        connection.setKeyspace(name);
        connection.setUsername("user");
        connection.setPassword("password");
        connection.setRole("admin");

        SortedMap<String, Object> classifier = new TreeMap<>();
        classifier.put(SCOPE, scope);
        if (tenantId != null) {
            classifier.put(TENANT_ID, tenantId);
        }

        CassandraDatabase database = new CassandraDatabase();
        database.setName(name);
        database.setConnectionProperties(connection);
        database.setClassifier(classifier);
        return database;
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
