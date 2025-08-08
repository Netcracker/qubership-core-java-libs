package org.qubership.maas.declarative.kafka.quarkus.client.config;

import org.qubership.maas.declarative.kafka.client.impl.common.constant.MaasKafkaCommonConstants;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;
import java.util.Optional;

@Singleton
public class MaasKafkaProps {

    @ConfigProperty(name = MaasKafkaCommonConstants.ACCEPTABLE_TENANTS)
    Optional<List<String>> acceptableTenants;

    @ConfigProperty(
            name = MaasKafkaCommonConstants.CONSUMER_THREAD_POOL_SIZE,
            defaultValue = MaasKafkaCommonConstants.CONSUMER_THREAD_POOL_SIZE_DEFAULT_VALUE
    )
    Integer consumerThreadPoolSize;

    @ConfigProperty(
            name = MaasKafkaCommonConstants.CONSUMER_POOL_DURATION,
            defaultValue = MaasKafkaCommonConstants.CONSUMER_POOL_DURATION_DEFAULT_VALUE
    )
    Integer consumerCommonPoolDuration;

    @ConfigProperty(
            name = MaasKafkaCommonConstants.MAAS_AGENT_URL,
            defaultValue = MaasKafkaCommonConstants.MAAS_AGENT_URL_DEFAULT_VALUE
    )
    String maasAgentUrl;

    @ConfigProperty(name = MaasKafkaCommonConstants.CONSUMER_AWAIT_TIMEOUT_AFTER_ERROR)
    Optional<List<Long>> awaitItmeoutAfterError;

    // tracing props
    @ConfigProperty(name = "maas.tracing.kafka.enabled", defaultValue = "true")
    Boolean tracingEnabled;

    @ConfigProperty(name = "quarkus.jaeger.propagation", defaultValue = "b3")
    String jaegerPropagation;

    @ConfigProperty(name = "maas.kafka.monitoring.enabled", defaultValue = "true")
    Boolean kafkaMonitoringEnabled;
}
