package org.qubership.cloud.dbaas.client.opensearch.metrics;

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
    "dbaas.opensearch.metrics.requests-seconds.type=SUMMARY",
    "dbaas.opensearch.metrics.requests-seconds.quantiles=0.1,0.25,0.5,0.75,0.95",
    "dbaas.opensearch.metrics.requests-seconds.quantile-histogram=true",
    "dbaas.api.opensearch.service.delimiter=-",
    "dbaas.api.opensearch.service.prefix=test",
    "dbaas.api.opensearch.tenant.prefix=tenant-{tenantId}-test",
    "dbaas.api.opensearch.tenant.delimiter=--"
})
class OpensearchMetricsRequestSecondsWithSummaryTypeTest extends AbstractOpensearchMetricsRequestSecondsTest {

    private static final String REQUESTS_SECONDS_QUANTILE_METRIC_NAME =
        OpensearchMetricsProvider.REQUESTS_SECONDS_METRIC_NAME + ".percentile";

    @Value("""
        #{T(java.util.Arrays).asList(
            '${dbaas.opensearch.metrics.requests-seconds.quantiles}'.split(',')
        ).size()}""")
    private int amountQuantiles;

    @Test
    void testOpensearchMetricRequestSecondsProperties() {
        var metricsProperties = opensearchConfigurationProperty.getMetrics();

        Assertions.assertNotNull(metricsProperties);
        Assertions.assertEquals(Boolean.TRUE, metricsProperties.getEnabled());

        var requestsSecondsMetricProperties = metricsProperties.getRequestsSeconds();

        Assertions.assertNotNull(requestsSecondsMetricProperties);
        Assertions.assertEquals(Boolean.TRUE, requestsSecondsMetricProperties.getEnabled());
        Assertions.assertEquals(OpensearchClientRequestsSecondsMetricType.SUMMARY, requestsSecondsMetricProperties.getType());
        Assertions.assertEquals(Duration.ofMillis(1), requestsSecondsMetricProperties.getMinimumExpectedValue());
        Assertions.assertEquals(Duration.ofSeconds(30), requestsSecondsMetricProperties.getMaximumExpectedValue());
        Assertions.assertEquals(
            List.of(0.1, 0.25, 0.5, 0.75, 0.95),
            requestsSecondsMetricProperties.getQuantiles()
        );
        Assertions.assertEquals(Boolean.TRUE, requestsSecondsMetricProperties.getQuantileHistogram());
        Assertions.assertEquals(List.of(), requestsSecondsMetricProperties.getHistogramBuckets());
    }

    @Test
    void testRequestsSecondsMetricAreRecordedDuringCreateIndexRequestToOpensearch() throws IOException {
        doTestRequestsSecondsMetricAreRecordedAfterCreateIndexRequestToOpensearch();

        doTestAuxiliaryRequestsSecondsMetricsAreRecordedAfterCreateIndexRequestToOpensearch(
            REQUESTS_SECONDS_QUANTILE_METRIC_NAME, amountQuantiles
        );
    }
}
