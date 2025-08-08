package com.netcracker.cloud.dbaas.client.opensearch.metrics;

import com.netcracker.cloud.dbaas.client.metrics.DatabaseMetricProperties;
import com.netcracker.cloud.dbaas.client.opensearch.DbaasOpensearchClient;
import com.netcracker.cloud.dbaas.client.opensearch.config.DbaaSOpensearchConfigurationProperty;
import com.netcracker.cloud.dbaas.client.opensearch.config.DbaasOpensearchConfiguration;
import com.netcracker.cloud.dbaas.client.opensearch.config.metrics.DbaasOpensearchMetricsAutoConfiguration;
import com.netcracker.cloud.dbaas.client.opensearch.config.metrics.TestMicrometerConfiguration;
import com.netcracker.cloud.dbaas.client.opensearch.restclient.configuration.OpensearchTestConfiguration;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch.indices.DeleteIndexRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@ContextConfiguration(classes = {
    OpensearchTestConfiguration.class,
    TestMicrometerConfiguration.class,
    DbaasOpensearchMetricsAutoConfiguration.class
})
abstract class AbstractOpensearchMetricsRequestSecondsTest {

    protected final Map<String, String> expectedMetricTags = Map.of(
        OpensearchClientRequestsSecondsObservationHandler.METHOD_TAG_NAME, "PUT",
        OpensearchClientRequestsSecondsObservationHandler.OUTCOME_TAG_NAME, "SUCCESS",
        OpensearchClientRequestsSecondsObservationHandler.STATUS_TAG_NAME, "200",
        OpensearchClientRequestsSecondsObservationHandler.OPERATION_TAG_NAME, OpensearchClientRequestsSecondsObservationHandler.DEFAULT_OPERATION_TAG_VALUE,
        DatabaseMetricProperties.ROLE_TAG, "admin",
        OpensearchClientRequestsSecondsObservationHandler.RESOURCE_PREFIX_TAG_NAME, "test"
    );

    @Autowired
    protected DbaaSOpensearchConfigurationProperty opensearchConfigurationProperty;

    @Autowired
    @Qualifier(DbaasOpensearchConfiguration.SERVICE_NATIVE_OPENSEARCH_CLIENT)
    protected DbaasOpensearchClient serviceDbaasOpensearchClient;

    @Autowired
    protected MeterRegistry meterRegistry;

    @BeforeEach
    void beforeEach() throws IOException {
        deleteTestIndex();
    }

    @AfterEach
    void afterEach() throws IOException {
        deleteTestIndex();
    }

    void deleteTestIndex() throws IOException {
        try {
            serviceDbaasOpensearchClient.getClient().indices().delete(
                new DeleteIndexRequest.Builder()
                    .index(serviceDbaasOpensearchClient.normalize(OpensearchTestConfiguration.TEST_INDEX))
                    .build()
            );
        } catch (OpenSearchException e) {
            log.info("Index {} already hasn't exist", OpensearchTestConfiguration.TEST_INDEX);
        }
    }

    void doTestRequestsSecondsMetricAreRecordedAfterCreateIndexRequestToOpensearch() throws IOException {
        serviceDbaasOpensearchClient.getClient()
            .indices()
            .create(builder -> builder.index(
                serviceDbaasOpensearchClient.normalize(OpensearchTestConfiguration.TEST_INDEX))
            );

        var meters = meterRegistry.getMeters();

        log.info("Recorded meters: {}", convertMetersToString(meters));


        var expectedMetricTagsEntrySet = expectedMetricTags.entrySet();

        Assertions.assertTrue(
            meters.stream()
                .map(Meter::getId)
                .anyMatch(meterId -> Optional.of(meterId)
                    .filter(id -> OpensearchMetricsProvider.REQUESTS_SECONDS_METRIC_NAME.equals(id.getName()))
                    .filter(id -> OpensearchMetricsProvider.REQUESTS_SECONDS_METRIC_DESCRIPTION.equals(id.getDescription()))
                    .filter(id -> id.getTags().stream()
                        .collect(Collectors.toMap(Tag::getKey, Tag::getValue))
                        .entrySet()
                        .containsAll(expectedMetricTagsEntrySet))
                    .isPresent()
                )
        );
    }

    void doTestAuxiliaryRequestsSecondsMetricsAreRecordedAfterCreateIndexRequestToOpensearch(
        String auxiliaryMetricName, int expectedAuxiliaryMetricsAmount
    ) {
        var actualAuxiliaryMetricsAmount = meterRegistry.getMeters().stream()
            .filter(meter -> auxiliaryMetricName.equals(meter.getId().getName()))
            .count();

        Assertions.assertEquals(0, actualAuxiliaryMetricsAmount % expectedAuxiliaryMetricsAmount);
    }

    protected String convertMetersToString(List<Meter> meters) {
        return meters.stream()
            .map(meter -> meter.getId() + ": " + meter.measure())
            .collect(Collectors.joining("\n\n"));
    }
}
