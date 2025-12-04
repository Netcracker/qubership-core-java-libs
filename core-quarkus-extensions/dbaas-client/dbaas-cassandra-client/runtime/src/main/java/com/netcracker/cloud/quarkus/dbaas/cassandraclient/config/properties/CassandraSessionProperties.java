package com.netcracker.cloud.quarkus.dbaas.cassandraclient.config.properties;

import com.netcracker.cloud.dbaas.client.cassandra.entity.DbaasCassandraMetricsProperties;
import com.netcracker.cloud.dbaas.client.cassandra.entity.DbaasCassandraProperties;
import com.netcracker.cloud.dbaas.client.cassandra.entity.metrics.MetricConfigurationParameters;
import com.netcracker.cloud.dbaas.client.cassandra.entity.metrics.NodeMetricsConfiguration;
import com.netcracker.cloud.dbaas.client.cassandra.entity.metrics.SessionMetricsConfiguration;
import com.netcracker.cloud.quarkus.dbaas.cassandraclient.config.properties.metrics.MetricParameters;
import com.netcracker.cloud.quarkus.dbaas.cassandraclient.config.properties.metrics.MetricsProperties;
import com.netcracker.cloud.quarkus.dbaas.cassandraclient.config.properties.metrics.NodeProperties;
import com.netcracker.cloud.quarkus.dbaas.cassandraclient.config.properties.metrics.SessionProperties;
import com.netcracker.cloud.quarkus.dbaas.cassandraclient.config.properties.migration.MigrationProperties;
import io.smallrye.config.WithName;

import java.util.Optional;

public interface CassandraSessionProperties {

    /**
     * Enabling SSL.
     */
    @WithName("ssl")
    Optional<Boolean> ssl();

    /**
     * The Request timeout in ms.
     */
    @WithName("requestTimeoutMs")
    Optional<Integer> requestTimeoutMs();

    /**
     * The Truststore path.
     */
    @WithName("truststorePath")
    Optional<String> truststorePath();

    /**
     * The Truststore password.
     */
    @WithName("truststorePassword")
    Optional<String> truststorePassword();

    /**
     * Whether to require validation that the hostname of the server certificate's common name matches
     * the hostname of the server being connected to.
     */
    @WithName("ssl-hostname-validation")
    Optional<Boolean> sslHostnameValidation();

    /**
     * Whether the slow replica avoidance should be enabled in the default LBP.
     */
    @WithName("lb-slow-replica-avoidance")
    Optional<Boolean> lbSlowReplicaAvoidance();

    /**
     * Metrics configuration parameters.
     */
    MetricsProperties metrics();

    /**
     * Migration configuration parameters.
     */
    MigrationProperties migration();

    default DbaasCassandraProperties getDbaasCassandraProperties() {
        DbaasCassandraProperties properties = new DbaasCassandraProperties();
        properties.setSsl(ssl().orElse(false));
        properties.setRequestTimeoutMs(requestTimeoutMs().orElse(0));
        properties.setTruststorePath(truststorePath().orElse(null));
        properties.setTruststorePassword(truststorePassword().orElse(null));
        properties.setSslHostnameValidation(sslHostnameValidation().orElse(null));
        properties.setLbSlowReplicaAvoidance(lbSlowReplicaAvoidance().orElse(null));

        setMetrics(properties.getMetrics(), metrics());
        return properties;
    }

    private static void setMetrics(DbaasCassandraMetricsProperties metrics,
                                   MetricsProperties metricsProperties) {
        metricsProperties.enabled().ifPresent(metrics::setEnabled);
        setSessionMetrics(metrics.getSession(), metricsProperties.session());
        setNodeMetrics(metrics.getNode(), metricsProperties.node());
    }

    private static void setSessionMetrics(SessionMetricsConfiguration sessionMetrics,
                                          SessionProperties sessionProperties) {
        sessionProperties.enabled().ifPresent(sessionMetrics::setEnabled);
        setMetricConfiguration(sessionMetrics.getCqlRequests(), sessionProperties.cqlRequests());
        setMetricConfiguration(sessionMetrics.getThrottling().getDelay(), sessionProperties.throttling());
        setMetricConfiguration(sessionMetrics.getContinuousCqlRequests(), sessionProperties.continuousCqlRequests());
        setMetricConfiguration(sessionMetrics.getGraphRequests(), sessionProperties.graphRequests());
    }

    private static void setNodeMetrics(NodeMetricsConfiguration nodeMetrics,
                                       NodeProperties nodeProperties) {
        nodeProperties.enabled().ifPresent(nodeMetrics::setEnabled);
        setMetricConfiguration(nodeMetrics.getCqlMessages(), nodeProperties.cqlMessages());
        setMetricConfiguration(nodeMetrics.getGraphMessages(), nodeProperties.graphMessages());
        nodeProperties.expireAfter().ifPresent(nodeMetrics::setExpireAfter);
    }

    private static void setMetricConfiguration(MetricConfigurationParameters metricConfiguration,
                                               MetricParameters metricParameters) {
        metricParameters.highestLatency().ifPresent(metricConfiguration::setHighestLatency);
        metricParameters.lowestLatency().ifPresent(metricConfiguration::setLowestLatency);
        metricParameters.significantDigits().ifPresent(metricConfiguration::setSignificantDigits);
        metricParameters.refreshInterval().ifPresent(metricConfiguration::setRefreshInterval);
        metricParameters.slo().ifPresent(metricConfiguration::setSlo);
    }
}
