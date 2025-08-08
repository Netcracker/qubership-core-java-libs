package com.netcracker.cloud.dbaas.client.opensearch.config.metrics;

import org.qubership.cloud.dbaas.client.opensearch.config.DbaaSOpensearchConfigurationProperty;
import org.qubership.cloud.dbaas.client.opensearch.entity.metrics.OpensearchClientRequestsSecondsMetricType;
import org.qubership.cloud.dbaas.client.opensearch.metrics.OpensearchMetricsProvider;
import org.qubership.cloud.dbaas.client.opensearch.restclient.configuration.OpensearchTestConfiguration;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;

import java.time.Duration;
import java.util.List;

@SpringBootTest(properties = {
    "cloud.microservice.name=test-app",
    "cloud.microservice.namespace=default",
    "dbaas.api.opensearch.service.delimiter=-",
    "dbaas.api.opensearch.service.prefix=test",
    "dbaas.api.opensearch.tenant.prefix=tenant-{tenantId}-test",
    "dbaas.api.opensearch.tenant.delimiter=--"})
@ContextConfiguration(classes = {
    OpensearchTestConfiguration.class,
    TestMicrometerConfiguration.class,
    DbaasOpensearchMetricsAutoConfiguration.class})
@Slf4j
class OpensearchMetricsConfigurationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DbaaSOpensearchConfigurationProperty opensearchConfigurationProperty;

    @Test
    void testOpensearchMetricBeansAreCreated() {
        Assertions.assertNotNull(applicationContext.getBean(MeterRegistry.class));
        Assertions.assertNotNull(applicationContext.getBean(OpensearchMetricsProvider.class));
    }

    @Test
    void testOpensearchDefaultMetricProperties() {
        var metricsProperties = opensearchConfigurationProperty.getMetrics();

        Assertions.assertNotNull(metricsProperties);
        Assertions.assertEquals(Boolean.TRUE, metricsProperties.getEnabled());

        var requestsSecondsMetricProperties = metricsProperties.getRequestsSeconds();

        Assertions.assertNotNull(requestsSecondsMetricProperties);
        Assertions.assertEquals(Boolean.TRUE, requestsSecondsMetricProperties.getEnabled());
        Assertions.assertEquals(OpensearchClientRequestsSecondsMetricType.SUMMARY, requestsSecondsMetricProperties.getType());
        Assertions.assertEquals(Duration.ofMillis(1), requestsSecondsMetricProperties.getMinimumExpectedValue());
        Assertions.assertEquals(Duration.ofSeconds(30), requestsSecondsMetricProperties.getMaximumExpectedValue());
        Assertions.assertEquals(List.of(), requestsSecondsMetricProperties.getQuantiles());
        Assertions.assertEquals(Boolean.FALSE, requestsSecondsMetricProperties.getQuantileHistogram());
        Assertions.assertEquals(List.of(), requestsSecondsMetricProperties.getHistogramBuckets());
    }
}
