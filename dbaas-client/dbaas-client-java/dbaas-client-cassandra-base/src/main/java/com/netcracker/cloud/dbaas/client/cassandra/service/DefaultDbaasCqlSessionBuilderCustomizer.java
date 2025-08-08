package com.netcracker.cloud.dbaas.client.cassandra.service;

import com.datastax.dse.driver.api.core.config.DseDriverOption;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.ProgrammaticDriverConfigLoaderBuilder;
import com.datastax.oss.driver.internal.core.loadbalancing.DcInferringLoadBalancingPolicy;
import com.datastax.oss.driver.internal.core.ssl.DefaultSslEngineFactory;
import com.datastax.oss.driver.internal.metrics.micrometer.MicrometerMetricsFactory;
import org.qubership.cloud.dbaas.client.cassandra.entity.DbaasCassandraProperties;
import org.qubership.cloud.dbaas.client.cassandra.entity.metrics.MetricConfigurationParameters;
import org.qubership.cloud.dbaas.client.cassandra.entity.metrics.NodeMetricsConfiguration;
import org.qubership.cloud.dbaas.client.cassandra.entity.metrics.SessionMetricsConfiguration;
import org.qubership.cloud.dbaas.client.cassandra.metrics.DbaasTaggingMetricIdGenerator;
import jakarta.annotation.Priority;

import java.time.Duration;

/**
 * The default session builder customizer with basic adjustments of a session.
 * It works first due to the highest priority (zero value)
 */
@Priority(0)
public class DefaultDbaasCqlSessionBuilderCustomizer implements DbaasCqlSessionBuilderCustomizer {

    public static final int DEFAULT_REQUEST_TIMEOUT_MS = 180000;
    private final DbaasCassandraProperties dbaasCassandraProperties;

    public DefaultDbaasCqlSessionBuilderCustomizer(DbaasCassandraProperties dbaasCassandraProperties) {
        this.dbaasCassandraProperties = dbaasCassandraProperties;
    }

    @Override
    public void customize(CqlSessionBuilder cqlSessionBuilder) {
        // default does nothing
    }

    @Override
    public void customize(ProgrammaticDriverConfigLoaderBuilder configLoader) {
        // in case there is need to override any value use system properties
        // see test CassandraSessionBuilderTest.testCassandraDriverOption_OverridingFromSystemProperty
        Duration requestTimeout = Duration.ofMillis(dbaasCassandraProperties.getRequestTimeoutMs() > 0
                ? dbaasCassandraProperties.getRequestTimeoutMs() : DEFAULT_REQUEST_TIMEOUT_MS);

        configLoader
                .withClass(DefaultDriverOption.LOAD_BALANCING_POLICY_CLASS, DcInferringLoadBalancingPolicy.class)
                .withDuration(DefaultDriverOption.REQUEST_TIMEOUT, requestTimeout)
                .withDuration(DefaultDriverOption.CONTROL_CONNECTION_TIMEOUT, requestTimeout)
                .withDuration(DefaultDriverOption.METADATA_SCHEMA_REQUEST_TIMEOUT, requestTimeout)
                .withDuration(DefaultDriverOption.REPREPARE_TIMEOUT, requestTimeout)
                .withDuration(DefaultDriverOption.CONNECTION_CONNECT_TIMEOUT, requestTimeout)
                .withDuration(DefaultDriverOption.CONNECTION_INIT_QUERY_TIMEOUT, requestTimeout)
                .withDuration(DefaultDriverOption.CONNECTION_SET_KEYSPACE_TIMEOUT, requestTimeout);

        if (dbaasCassandraProperties.isSsl()) {
            // in case customizer brings the custom ssl settings - this custom settings have priority
            // see com.datastax.oss.driver.internal.core.context.DefaultDriverContext.buildSslEngineFactory
            configLoader.withClass(DefaultDriverOption.SSL_ENGINE_FACTORY_CLASS, DefaultSslEngineFactory.class);

            // if not defined there is used default: java.home/lib/security/cacerts
            configLoader.withString(DefaultDriverOption.SSL_TRUSTSTORE_PATH,
                    dbaasCassandraProperties.getTruststorePath());

            // if not defined there is used default: changeit
            configLoader.withString(DefaultDriverOption.SSL_TRUSTSTORE_PASSWORD,
                    dbaasCassandraProperties.getTruststorePassword());

            // if not defined there is used default: true
            if (dbaasCassandraProperties.getSslHostnameValidation() != null) {
                configLoader.withBoolean(DefaultDriverOption.SSL_HOSTNAME_VALIDATION,
                        dbaasCassandraProperties.getSslHostnameValidation());
            }
        }

        // if not defined there is used default: false
        if (dbaasCassandraProperties.getResolveContactPoints() != null) {
            // when resolve-contact-points=false then host name will be resolved again every time the driver opens a new connection
            configLoader.withBoolean(DefaultDriverOption.RESOLVE_CONTACT_POINTS, false);
        }

        // if not defined there is used default: true
        if (dbaasCassandraProperties.getLbSlowReplicaAvoidance() != null) {
            configLoader.withBoolean(DefaultDriverOption.LOAD_BALANCING_POLICY_SLOW_AVOIDANCE,
                    dbaasCassandraProperties.getLbSlowReplicaAvoidance());
        }
        if (dbaasCassandraProperties.getMetrics().getEnabled()) {
            customizeMetrics(configLoader);
        }
    }

    private void customizeMetrics(ProgrammaticDriverConfigLoaderBuilder configLoader) {
        configLoader.withClass(DefaultDriverOption.METRICS_FACTORY_CLASS, MicrometerMetricsFactory.class);
        configLoader.withClass(DefaultDriverOption.METRICS_ID_GENERATOR_CLASS, DbaasTaggingMetricIdGenerator.class);
        configLoader.withString(DefaultDriverOption.METRICS_ID_GENERATOR_PREFIX, "cassandra");

        // Session metrics
        SessionMetricsConfiguration sessionMetricsConfiguration = dbaasCassandraProperties.getMetrics().getSession();
        configLoader.withStringList(DefaultDriverOption.METRICS_SESSION_ENABLED, sessionMetricsConfiguration.getEnabled());

        MetricConfigurationParameters cqlRequests = sessionMetricsConfiguration.getCqlRequests();
        configLoader.withDuration(DefaultDriverOption.METRICS_SESSION_CQL_REQUESTS_HIGHEST, cqlRequests.getHighestLatency());
        configLoader.withDuration(DefaultDriverOption.METRICS_SESSION_CQL_REQUESTS_LOWEST, cqlRequests.getLowestLatency());
        configLoader.withInt(DefaultDriverOption.METRICS_SESSION_CQL_REQUESTS_DIGITS, cqlRequests.getSignificantDigits());
        configLoader.withDuration(DefaultDriverOption.METRICS_SESSION_CQL_REQUESTS_INTERVAL, cqlRequests.getRefreshInterval());
        if (cqlRequests.getSlo() != null) {
            configLoader.withDurationList(DefaultDriverOption.METRICS_SESSION_CQL_REQUESTS_SLO, cqlRequests.getSlo());
        }

        MetricConfigurationParameters throttlingDelay = sessionMetricsConfiguration.getThrottling().getDelay();
        configLoader.withDuration(DefaultDriverOption.METRICS_SESSION_THROTTLING_HIGHEST, throttlingDelay.getHighestLatency());
        configLoader.withDuration(DefaultDriverOption.METRICS_SESSION_THROTTLING_LOWEST, throttlingDelay.getLowestLatency());
        configLoader.withInt(DefaultDriverOption.METRICS_SESSION_THROTTLING_DIGITS, throttlingDelay.getSignificantDigits());
        configLoader.withDuration(DefaultDriverOption.METRICS_SESSION_THROTTLING_INTERVAL, throttlingDelay.getRefreshInterval());
        if (throttlingDelay.getSlo() != null) {
            configLoader.withDurationList(DefaultDriverOption.METRICS_SESSION_THROTTLING_SLO, throttlingDelay.getSlo());
        }

        MetricConfigurationParameters continuousCqlRequests = sessionMetricsConfiguration.getContinuousCqlRequests();
        configLoader.withDuration(DseDriverOption.CONTINUOUS_PAGING_METRICS_SESSION_CQL_REQUESTS_HIGHEST, continuousCqlRequests.getHighestLatency());
        configLoader.withDuration(DseDriverOption.CONTINUOUS_PAGING_METRICS_SESSION_CQL_REQUESTS_LOWEST, continuousCqlRequests.getLowestLatency());
        configLoader.withInt(DseDriverOption.CONTINUOUS_PAGING_METRICS_SESSION_CQL_REQUESTS_DIGITS, continuousCqlRequests.getSignificantDigits());
        configLoader.withDuration(DseDriverOption.CONTINUOUS_PAGING_METRICS_SESSION_CQL_REQUESTS_INTERVAL, continuousCqlRequests.getRefreshInterval());
        if (continuousCqlRequests.getSlo() != null) {
            configLoader.withDurationList(DseDriverOption.CONTINUOUS_PAGING_METRICS_SESSION_CQL_REQUESTS_SLO, continuousCqlRequests.getSlo());
        }

        MetricConfigurationParameters graphRequests = sessionMetricsConfiguration.getGraphRequests();
        configLoader.withDuration(DseDriverOption.METRICS_SESSION_GRAPH_REQUESTS_HIGHEST, graphRequests.getHighestLatency());
        configLoader.withDuration(DseDriverOption.METRICS_SESSION_GRAPH_REQUESTS_LOWEST, graphRequests.getLowestLatency());
        configLoader.withInt(DseDriverOption.METRICS_SESSION_GRAPH_REQUESTS_DIGITS, graphRequests.getSignificantDigits());
        configLoader.withDuration(DseDriverOption.METRICS_SESSION_GRAPH_REQUESTS_INTERVAL, graphRequests.getRefreshInterval());
        if (graphRequests.getSlo() != null) {
            configLoader.withDurationList(DseDriverOption.METRICS_SESSION_GRAPH_REQUESTS_SLO, graphRequests.getSlo());
        }

        // Node metrics
        NodeMetricsConfiguration nodeMetricsConfiguration = dbaasCassandraProperties.getMetrics().getNode();
        configLoader.withStringList(DefaultDriverOption.METRICS_NODE_ENABLED, nodeMetricsConfiguration.getEnabled());

        MetricConfigurationParameters cqlMessages = nodeMetricsConfiguration.getCqlMessages();
        configLoader.withDuration(DefaultDriverOption.METRICS_NODE_CQL_MESSAGES_HIGHEST, cqlMessages.getHighestLatency());
        configLoader.withDuration(DefaultDriverOption.METRICS_NODE_CQL_MESSAGES_LOWEST, cqlMessages.getLowestLatency());
        configLoader.withInt(DefaultDriverOption.METRICS_NODE_CQL_MESSAGES_DIGITS, cqlMessages.getSignificantDigits());
        configLoader.withDuration(DefaultDriverOption.METRICS_NODE_CQL_MESSAGES_INTERVAL, cqlMessages.getRefreshInterval());
        if (cqlMessages.getSlo() != null) {
            configLoader.withDurationList(DefaultDriverOption.METRICS_NODE_CQL_MESSAGES_SLO, cqlMessages.getSlo());
        }

        MetricConfigurationParameters graphMessages = nodeMetricsConfiguration.getGraphMessages();
        configLoader.withDuration(DseDriverOption.METRICS_NODE_GRAPH_MESSAGES_HIGHEST, graphMessages.getHighestLatency());
        configLoader.withDuration(DseDriverOption.METRICS_NODE_GRAPH_MESSAGES_LOWEST, graphMessages.getLowestLatency());
        configLoader.withInt(DseDriverOption.METRICS_NODE_GRAPH_MESSAGES_DIGITS, graphMessages.getSignificantDigits());
        configLoader.withDuration(DseDriverOption.METRICS_NODE_GRAPH_MESSAGES_INTERVAL, graphMessages.getRefreshInterval());
        if (graphMessages.getSlo() != null) {
            configLoader.withDurationList(DseDriverOption.METRICS_NODE_GRAPH_MESSAGES_SLO, graphMessages.getSlo());
        }
        configLoader.withDuration(DefaultDriverOption.METRICS_NODE_EXPIRE_AFTER, nodeMetricsConfiguration.getExpireAfter());
    }
}
