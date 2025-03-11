package org.qubership.cloud.quarkus.dbaas.opensearch.client.it;

import org.qubership.cloud.dbaas.client.metrics.DatabaseMetricProperties;
import org.qubership.cloud.dbaas.client.metrics.MetricsProvider;
import org.qubership.cloud.dbaas.client.opensearch.DbaasOpensearchClient;
import org.qubership.cloud.dbaas.client.opensearch.entity.OpensearchIndex;
import org.qubership.cloud.dbaas.client.opensearch.entity.metrics.OpensearchClientRequestsSecondsMetricType;
import org.qubership.cloud.dbaas.client.opensearch.metrics.OpensearchClientRequestsSecondsObservationHandler;
import org.qubership.cloud.dbaas.client.opensearch.metrics.OpensearchMetricsProvider;
import org.qubership.cloud.quarkus.dbaas.opensearch.client.DbaasOpensearchConfiguration;
import org.qubership.cloud.quarkus.dbaas.opensearch.client.config.DbaaSOpensearchConfigurationProperty;
import org.qubership.cloud.quarkus.dbaas.opensearch.client.it.configuration.ContainerLogicalDbProvider;
import org.qubership.cloud.quarkus.dbaas.opensearch.client.it.configuration.OpensearchContainerResource;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch.indices.DeleteIndexRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@QuarkusTest
@TestProfile(MetricsTest.MetricsTestProfile.class)
class MetricsTest {

    private final Map<String, String> expectedMetricTags = Map.of(
        OpensearchClientRequestsSecondsObservationHandler.METHOD_TAG_NAME, "PUT",
        OpensearchClientRequestsSecondsObservationHandler.OUTCOME_TAG_NAME, "SUCCESS",
        OpensearchClientRequestsSecondsObservationHandler.STATUS_TAG_NAME, "200",
        OpensearchClientRequestsSecondsObservationHandler.OPERATION_TAG_NAME, OpensearchClientRequestsSecondsObservationHandler.DEFAULT_OPERATION_TAG_VALUE,
        DatabaseMetricProperties.ROLE_TAG, ContainerLogicalDbProvider.ADMIN_ROLE,
        OpensearchClientRequestsSecondsObservationHandler.RESOURCE_PREFIX_TAG_NAME, ContainerLogicalDbProvider.TEST_PREFIX
    );

    @Inject
    Instance<MeterRegistry> meterRegistry;

    @Inject
    Instance<MetricsProvider<OpensearchIndex>> opensearchMetricsProvider;

    @Inject
    DbaaSOpensearchConfigurationProperty dbaaSOpensearchConfigurationProperty;

    @Inject
    @Named(DbaasOpensearchConfiguration.SERVICE_NATIVE_OPENSEARCH_CLIENT)
    DbaasOpensearchClient serviceDbaasOpensearchClient;

    @BeforeEach
    void beforeEach() throws IOException {
        deleteTestIndex();
    }

    @AfterEach
    void afterEach() throws IOException {
        deleteTestIndex();
    }

    private void deleteTestIndex() throws IOException {
        try {
            serviceDbaasOpensearchClient.getClient().indices().delete(
                new DeleteIndexRequest.Builder()
                    .index(serviceDbaasOpensearchClient.normalize(ContainerLogicalDbProvider.TEST_INDEX))
                    .build()
            );
        } catch (OpenSearchException e) {
            log.info("Index {} already hasn't exist", ContainerLogicalDbProvider.TEST_INDEX);
        }
    }
    
    @Test
    void checkOpensearchMetricsProviderExistsInCdi() {
        Assertions.assertFalse(meterRegistry.isUnsatisfied());
        Assertions.assertFalse(opensearchMetricsProvider.isUnsatisfied());
    }

    @Test
    void checkDefaultValuesOfOpensearchMetricsProperties() {
        var dbaasOpensearchMetricsProperties = dbaaSOpensearchConfigurationProperty.getMetrics()
            .toDbaasOpensearchMetricsProperties();

        Assertions.assertNotNull(dbaasOpensearchMetricsProperties);
        Assertions.assertEquals(Boolean.TRUE, dbaasOpensearchMetricsProperties.getEnabled());

        var requestsSecondsMetricProperties = dbaasOpensearchMetricsProperties.getRequestsSeconds();

        Assertions.assertNotNull(requestsSecondsMetricProperties);
        Assertions.assertEquals(Boolean.TRUE, requestsSecondsMetricProperties.getEnabled());
        Assertions.assertEquals(OpensearchClientRequestsSecondsMetricType.SUMMARY, requestsSecondsMetricProperties.getType());
        Assertions.assertEquals(Duration.ofMillis(1), requestsSecondsMetricProperties.getMinimumExpectedValue());
        Assertions.assertEquals(Duration.ofSeconds(30), requestsSecondsMetricProperties.getMaximumExpectedValue());
        Assertions.assertEquals(List.of(), requestsSecondsMetricProperties.getQuantiles());
        Assertions.assertEquals(Boolean.FALSE, requestsSecondsMetricProperties.getQuantileHistogram());
        Assertions.assertEquals(List.of(), requestsSecondsMetricProperties.getHistogramBuckets());
    }

    @Test
    void testRequestsSecondsMetricAreRecordedDuringCreateIndexRequestToOpensearch() throws IOException {
        serviceDbaasOpensearchClient.getClient()
            .indices()
            .create(builder -> builder.index(
                serviceDbaasOpensearchClient.normalize(ContainerLogicalDbProvider.TEST_INDEX))
            );

        var expectedMetricTagsEntrySet = expectedMetricTags.entrySet();

        Assertions.assertTrue(
            meterRegistry.get().getMeters().stream()
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

    @NoArgsConstructor
    protected static final class MetricsTestProfile implements QuarkusTestProfile {

        @Override
        public List<TestResourceEntry> testResources() {
            return List.of(new TestResourceEntry(OpensearchContainerResource.class));
        }

        @Override
        public boolean disableGlobalTestResources() {
            return true;
        }
    }
}
