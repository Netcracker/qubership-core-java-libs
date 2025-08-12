package com.netcracker.cloud.dbaas.client.opensearch.metrics;

import com.netcracker.cloud.dbaas.client.metrics.DatabaseMetricProperties;
import io.micrometer.common.KeyValue;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.httpcomponents.hc5.ApacheHttpClientContext;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.hc.core5.http.HttpRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class OpensearchClientRequestsSecondsObservationHandlerTest {

    private final Map<String, String> baseMetricTags = Map.of(
        OpensearchClientRequestsSecondsObservationHandler.RESOURCE_PREFIX_TAG_NAME, "test",
        DatabaseMetricProperties.ROLE_TAG, "admin"
    );

    private final Timer.Builder spyRequestSecondsBaseMetricBuilder =
        Mockito.spy(
            Timer.builder(OpensearchMetricsProvider.REQUESTS_SECONDS_METRIC_NAME)
                .description(OpensearchMetricsProvider.REQUESTS_SECONDS_METRIC_DESCRIPTION)
                .publishPercentiles(0.1, 0.25, 0.5, 0.75, 0.95)
        );

    @Mock
    private ApacheHttpClientContext mockApacheHttpClientContext;

    @Spy
    private MeterRegistry spyMeterRegistry = new SimpleMeterRegistry();

    private OpensearchClientRequestsSecondsObservationHandler opensearchClientRequestsSecondsObservationHandler;

    @BeforeEach
    void beforeEach() {
        MockitoAnnotations.openMocks(this);

        opensearchClientRequestsSecondsObservationHandler = new OpensearchClientRequestsSecondsObservationHandler(
            baseMetricTags, spyMeterRegistry, spyRequestSecondsBaseMetricBuilder
        );
    }

    @Test
    void testSupportsContext() {
        Assertions.assertTrue(
            opensearchClientRequestsSecondsObservationHandler.supportsContext(mockApacheHttpClientContext)
        );
    }

    @Test
    void testOnStart() {
        opensearchClientRequestsSecondsObservationHandler.onStart(mockApacheHttpClientContext);

        Mockito.verify(mockApacheHttpClientContext).put(
            ArgumentMatchers.eq(OpensearchClientRequestsSecondsObservationHandler.START_TIME_IN_MILLIS),
            ArgumentMatchers.anyLong()
        );
    }

    @Test
    void testOnStopWhenStartTimeInMillisIsAbsentInContext() {
        opensearchClientRequestsSecondsObservationHandler.onStop(mockApacheHttpClientContext);

        Mockito.verify(mockApacheHttpClientContext).get(
            OpensearchClientRequestsSecondsObservationHandler.START_TIME_IN_MILLIS
        );

        Mockito.verifyNoMoreInteractions(mockApacheHttpClientContext);
        Mockito.verifyNoInteractions(spyMeterRegistry);
    }

    @Test
    void testOnStopWhenStartTimeInMillisIsPresentInContext() {
        var randomStartTimeInMillis = System.currentTimeMillis() - (100 + RandomGenerator.getDefault().nextInt(1400));

        Mockito.doReturn(randomStartTimeInMillis)
            .when(mockApacheHttpClientContext).get(
                OpensearchClientRequestsSecondsObservationHandler.START_TIME_IN_MILLIS
            );

        Mockito.doReturn(KeyValue.of(OpensearchClientRequestsSecondsObservationHandler.METHOD_TAG_NAME, "GET"))
            .when(mockApacheHttpClientContext).getLowCardinalityKeyValue(OpensearchClientRequestsSecondsObservationHandler.METHOD_TAG_NAME);

        Mockito.doReturn(KeyValue.of(OpensearchClientRequestsSecondsObservationHandler.OUTCOME_TAG_NAME, "SUCCESS"))
            .when(mockApacheHttpClientContext).getLowCardinalityKeyValue(OpensearchClientRequestsSecondsObservationHandler.OUTCOME_TAG_NAME);

        Mockito.doReturn(KeyValue.of(OpensearchClientRequestsSecondsObservationHandler.STATUS_TAG_NAME, "200"))
            .when(mockApacheHttpClientContext).getLowCardinalityKeyValue(OpensearchClientRequestsSecondsObservationHandler.STATUS_TAG_NAME);

        var mockHttpRequest = Mockito.mock(HttpRequest.class);

        Mockito.doReturn("/test-index?include_defaults=true")
            .when(mockHttpRequest).getPath();

        Mockito.doReturn(mockHttpRequest)
            .when(mockApacheHttpClientContext).getCarrier();

        var capturedSpyTimer = new Timer[1];

        Mockito.doAnswer(invocation -> {
                var spyTimer = Mockito.spy((Timer) invocation.callRealMethod());
                capturedSpyTimer[0] = spyTimer;
                return spyTimer;
            })
            .when(spyRequestSecondsBaseMetricBuilder).register(spyMeterRegistry);

        opensearchClientRequestsSecondsObservationHandler.onStop(mockApacheHttpClientContext);

        Mockito.verify(spyRequestSecondsBaseMetricBuilder).tags(ArgumentMatchers.any(Iterable.class));
        Mockito.verify(spyRequestSecondsBaseMetricBuilder).register(spyMeterRegistry);
        Mockito.verify(capturedSpyTimer[0]).record(ArgumentMatchers.any(Duration.class));

        var expectedMetricTagsEntrySet = Stream.of(
                baseMetricTags,
                Map.of(
                    OpensearchClientRequestsSecondsObservationHandler.METHOD_TAG_NAME, "GET",
                    OpensearchClientRequestsSecondsObservationHandler.OUTCOME_TAG_NAME, "SUCCESS",
                    OpensearchClientRequestsSecondsObservationHandler.STATUS_TAG_NAME, "200",
                    OpensearchClientRequestsSecondsObservationHandler.OPERATION_TAG_NAME, "index"
                )
            )
            .map(Map::entrySet)
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());

        Assertions.assertTrue(
            spyMeterRegistry.getMeters().stream()
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
}
