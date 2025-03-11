package org.qubership.cloud.quarkus.dbaas.cassandraclient.config.properties;

import org.qubership.cloud.dbaas.client.cassandra.entity.DbaasCassandraMetricsProperties;
import org.qubership.cloud.dbaas.client.cassandra.entity.DbaasCassandraProperties;
import org.qubership.cloud.dbaas.client.cassandra.entity.metrics.MetricConfigurationParameters;
import org.qubership.cloud.dbaas.client.cassandra.entity.metrics.NodeMetricsConfiguration;
import org.qubership.cloud.dbaas.client.cassandra.entity.metrics.SessionMetricsConfiguration;
import org.qubership.cloud.quarkus.dbaas.cassandraclient.config.properties.metrics.MetricParameters;
import org.qubership.cloud.quarkus.dbaas.cassandraclient.config.properties.metrics.MetricsProperties;
import org.qubership.cloud.quarkus.dbaas.cassandraclient.config.properties.metrics.NodeProperties;
import org.qubership.cloud.quarkus.dbaas.cassandraclient.config.properties.metrics.SessionProperties;
import org.qubership.cloud.quarkus.dbaas.cassandraclient.config.properties.migration.MigrationProperties;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import lombok.Getter;

import java.util.Optional;

@Getter
@ConfigGroup
public class CassandraSessionProperties {

    /**
     * Enabling SSL.
     */
    @ConfigItem(name = "ssl")
    Optional<Boolean> ssl;

    /**
     * The Request timeout in ms.
     */
    @ConfigItem(name = "requestTimeoutMs")
    Optional<Integer> requestTimeoutMs;

    /**
     * The Truststore path.
     */
    @ConfigItem(name = "truststorePath")
    Optional<String> truststorePath;

    /**
     * The Truststore password.
     */
    @ConfigItem(name = "truststorePassword")
    Optional<String> truststorePassword;

    /**
     * Whether to require validation that the hostname of the server certificate's common name matches
     * the hostname of the server being connected to.
     */
    @ConfigItem(name = "ssl-hostname-validation")
    Optional<Boolean> sslHostnameValidation;

    /**
     * Whether the slow replica avoidance should be enabled in the default LBP.
     */
    @ConfigItem(name = "lb-slow-replica-avoidance")
    Optional<Boolean> lbSlowReplicaAvoidance;

    /**
     * Metrics configuration parameters.
     */
    @ConfigItem
    MetricsProperties metrics;

    /**
     * Migration configuration parameters.
     */
    @ConfigItem
    MigrationProperties migration;

    public DbaasCassandraProperties getDbaasCassandraProperties() {
        DbaasCassandraProperties properties = new DbaasCassandraProperties();
        properties.setSsl(ssl.orElse(false));
        properties.setRequestTimeoutMs(requestTimeoutMs.orElse(0));
        properties.setTruststorePath(truststorePath.orElse(null));
        properties.setTruststorePassword(truststorePassword.orElse(null));
        properties.setSslHostnameValidation(sslHostnameValidation.orElse(null));
        properties.setLbSlowReplicaAvoidance(lbSlowReplicaAvoidance.orElse(null));

        setMetrics(properties.getMetrics(), metrics);
        return properties;
    }

    private void setMetrics(DbaasCassandraMetricsProperties metrics, MetricsProperties metricsProperties) {
        metricsProperties.enabled().ifPresent(metrics::setEnabled);
        setSessionMetrics(metrics.getSession(), metricsProperties.session());
        setNodeMetrics(metrics.getNode(), metricsProperties.node());
    }

    private void setSessionMetrics(SessionMetricsConfiguration sessionMetrics, SessionProperties sessionProperties) {
        sessionProperties.enabled().ifPresent(sessionMetrics::setEnabled);
        setMetricConfiguration(sessionMetrics.getCqlRequests(), sessionProperties.cqlRequests());
        setMetricConfiguration(sessionMetrics.getThrottling().getDelay(), sessionProperties.throttling());
        setMetricConfiguration(sessionMetrics.getContinuousCqlRequests(), sessionProperties.continuousCqlRequests());
        setMetricConfiguration(sessionMetrics.getGraphRequests(), sessionProperties.graphRequests());
    }

    private void setNodeMetrics(NodeMetricsConfiguration nodeMetrics, NodeProperties nodeProperties) {
        nodeProperties.enabled().ifPresent(nodeMetrics::setEnabled);
        setMetricConfiguration(nodeMetrics.getCqlMessages(), nodeProperties.cqlMessages());
        setMetricConfiguration(nodeMetrics.getGraphMessages(), nodeProperties.graphMessages());
        nodeProperties.expireAfter().ifPresent(nodeMetrics::setExpireAfter);

    }

    private void setMetricConfiguration(MetricConfigurationParameters metricConfiguration, MetricParameters metricParameters) {
        metricParameters.highestLatency().ifPresent(metricConfiguration::setHighestLatency);
        metricParameters.lowestLatency().ifPresent(metricConfiguration::setLowestLatency);
        metricParameters.significantDigits().ifPresent(metricConfiguration::setSignificantDigits);
        metricParameters.refreshInterval().ifPresent(metricConfiguration::setRefreshInterval);
        metricParameters.slo().ifPresent(metricConfiguration::setSlo);
    }
}
