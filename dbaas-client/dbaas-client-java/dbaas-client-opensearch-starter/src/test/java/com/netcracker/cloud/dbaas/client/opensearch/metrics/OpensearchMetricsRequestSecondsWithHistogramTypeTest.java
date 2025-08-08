package com.netcracker.cloud.dbaas.client.opensearch.metrics;

import org.qubership.cloud.dbaas.client.opensearch.entity.metrics.OpensearchClientRequestsSecondsMetricType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

@SpringBootTest(properties = {
    "cloud.microservice.name=test-app",
    "cloud.microservice.namespace=default",
    "dbaas.opensearch.metrics.requests-seconds.type=HISTOGRAM",
    "dbaas.opensearch.metrics.requests-seconds.histogram-buckets=100ms,500ms,1000ms,2000ms,5000ms",
    "dbaas.api.opensearch.service.delimiter=-",
    "dbaas.api.opensearch.service.prefix=test",
    "dbaas.api.opensearch.tenant.prefix=tenant-{tenantId}-test",
    "dbaas.api.opensearch.tenant.delimiter=--"
})
class OpensearchMetricsRequestSecondsWithHistogramTypeTest extends AbstractOpensearchMetricsRequestSecondsTest {

    private static final String REQUESTS_SECONDS_BUCKET_METRIC_NAME =
        OpensearchMetricsProvider.REQUESTS_SECONDS_METRIC_NAME + ".histogram";

    @Value("""
        #{T(java.util.Arrays).asList(
            '${dbaas.opensearch.metrics.requests-seconds.histogram-buckets}'.split(',')
        ).size()}""")
    private int amountHistogramBuckets;

    @Test
    void testOpensearchMetricRequestSecondsProperties() {
        var metricsProperties = opensearchConfigurationProperty.getMetrics();

        Assertions.assertNotNull(metricsProperties);
        Assertions.assertEquals(Boolean.TRUE, metricsProperties.getEnabled());

        var requestsSecondsMetricProperties = metricsProperties.getRequestsSeconds();

        Assertions.assertNotNull(requestsSecondsMetricProperties);
        Assertions.assertEquals(Boolean.TRUE, requestsSecondsMetricProperties.getEnabled());
        Assertions.assertEquals(OpensearchClientRequestsSecondsMetricType.HISTOGRAM, requestsSecondsMetricProperties.getType());
        Assertions.assertEquals(Duration.ofMillis(1), requestsSecondsMetricProperties.getMinimumExpectedValue());
        Assertions.assertEquals(Duration.ofSeconds(30), requestsSecondsMetricProperties.getMaximumExpectedValue());
        Assertions.assertEquals(List.of(), requestsSecondsMetricProperties.getQuantiles());
        Assertions.assertEquals(Boolean.FALSE, requestsSecondsMetricProperties.getQuantileHistogram());
        Assertions.assertEquals(
            List.of(
                Duration.ofMillis(100),
                Duration.ofMillis(500),
                Duration.ofMillis(1000),
                Duration.ofMillis(2000),
                Duration.ofMillis(5000)
            ),
            requestsSecondsMetricProperties.getHistogramBuckets()
        );
    }

    @Test
    void testRequestsSecondsMetricAreRecordedDuringCreateIndexRequestToOpensearch() throws IOException {
        doTestRequestsSecondsMetricAreRecordedAfterCreateIndexRequestToOpensearch();

        doTestAuxiliaryRequestsSecondsMetricsAreRecordedAfterCreateIndexRequestToOpensearch(
            REQUESTS_SECONDS_BUCKET_METRIC_NAME, amountHistogramBuckets
        );
    }
}
