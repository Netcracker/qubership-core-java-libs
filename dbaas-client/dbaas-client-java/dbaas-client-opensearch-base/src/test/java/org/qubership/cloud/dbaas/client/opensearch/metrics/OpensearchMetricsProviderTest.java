package org.qubership.cloud.dbaas.client.opensearch.metrics;

import org.qubership.cloud.dbaas.client.exceptions.MetricsRegistrationException;
import org.qubership.cloud.dbaas.client.metrics.DatabaseMetricProperties;
import org.qubership.cloud.dbaas.client.opensearch.entity.DbaasOpensearchMetricsProperties;
import org.qubership.cloud.dbaas.client.opensearch.entity.metrics.OpensearchClientRequestsSecondsMetricType;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.httpcomponents.hc5.ObservationExecChainHandler;
import org.apache.hc.client5.http.async.AsyncExecChainHandler;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

class OpensearchMetricsProviderTest {

    @Mock
    private DatabaseMetricProperties mockDatabaseMetricProperties;

    @Mock
    private Map<String, Object> mockExtraParameters;

    @Mock
    private HttpAsyncClientBuilder mockHttpClientBuilder;

    @Mock
    private MeterRegistry mockMeterRegistry;

    @Spy
    private DbaasOpensearchMetricsProperties spyOpensearchMetricsProperties;

    private OpensearchMetricsProvider opensearchMetricsProvider;

    @BeforeEach
    void beforeEach() {
        MockitoAnnotations.openMocks(this);

        opensearchMetricsProvider = new OpensearchMetricsProvider(mockMeterRegistry, spyOpensearchMetricsProperties);

        Mockito.doReturn(mockHttpClientBuilder)
            .when(mockExtraParameters).get(OpensearchMetricsProvider.HTTP_CLIENT_BUILDER);

        Mockito.doReturn(mockExtraParameters)
            .when(mockDatabaseMetricProperties).getExtraParameters();
    }

    @Test
    void testDoNotRegisterMetricsWhenMetricPropertiesAreDisabled() throws MetricsRegistrationException {
        spyOpensearchMetricsProperties.setEnabled(false);

        doTestDoNotRegisterMetrics();
    }

    @Test
    void testDoNotRegisterRequestSecondsMetricWhenRequestSecondsMetricPropertiesAreDisabled() throws MetricsRegistrationException {
        spyOpensearchMetricsProperties.getRequestsSeconds().setEnabled(false);

        doTestDoNotRegisterMetrics();
    }

    @Test
    void testThrowMetricsRegistrationExceptionWhenRegisterRequestSecondsMetricWithAbsentHttpClientBuilder() {
        Mockito.doReturn(null)
            .when(mockExtraParameters).get(OpensearchMetricsProvider.HTTP_CLIENT_BUILDER);

        Assertions.assertThrows(
            MetricsRegistrationException.class,
            () -> opensearchMetricsProvider.registerMetrics(mockDatabaseMetricProperties)
        );
    }

    @Test
    void testThrowMetricsRegistrationExceptionWhenRegisterRequestSecondsMetricWithMetricTypeAsNull() {
        spyOpensearchMetricsProperties.getRequestsSeconds().setType(null);

        Assertions.assertThrows(
            MetricsRegistrationException.class,
            () -> opensearchMetricsProvider.registerMetrics(mockDatabaseMetricProperties)
        );
    }

    @Test
    void testRegisterRequestSecondsMetricForSummaryMetricType() throws MetricsRegistrationException {
        doTestRegisterRequestSecondsMetricForSummaryMetricType();
    }

    @Test
    void testRegisterRequestSecondsMetricForSummaryMetricTypeAndQuntileHistogram() throws MetricsRegistrationException {
        spyOpensearchMetricsProperties.getRequestsSeconds().setQuantileHistogram(true);

        doTestRegisterRequestSecondsMetricForSummaryMetricType();
    }

    @Test
    void testRegisterRequestSecondsMetricForHistogramMetricType() throws MetricsRegistrationException {
        var requestsSecondsProperties = spyOpensearchMetricsProperties.getRequestsSeconds();

        requestsSecondsProperties.setType(OpensearchClientRequestsSecondsMetricType.HISTOGRAM);

        doTestRegisterRequestSecondsMetricWithVerificationsOnSpyMetricBuilder(spyTimerBuilder -> {
            Mockito.verify(spyTimerBuilder, Mockito.never()).publishPercentiles(
                ArgumentMatchers.any(double[].class)
            );
            Mockito.verify(spyTimerBuilder, Mockito.never()).publishPercentileHistogram(
                ArgumentMatchers.anyBoolean()
            );
            Mockito.verify(spyTimerBuilder).serviceLevelObjectives(
                requestsSecondsProperties.getHistogramBuckets().toArray(Duration[]::new)
            );
        });
    }

    protected void doTestDoNotRegisterMetrics() throws MetricsRegistrationException {
        opensearchMetricsProvider.registerMetrics(mockDatabaseMetricProperties);

        Mockito.verify(mockDatabaseMetricProperties, Mockito.never())
            .getExtraParameters();

        Mockito.verify(mockExtraParameters, Mockito.never())
            .get(OpensearchMetricsProvider.HTTP_CLIENT_BUILDER);

        Mockito.verify(mockHttpClientBuilder, Mockito.never())
            .addExecInterceptorLast(ArgumentMatchers.anyString(), ArgumentMatchers.any(AsyncExecChainHandler.class));
    }

    protected void doTestRegisterRequestSecondsMetricForSummaryMetricType() throws MetricsRegistrationException {
        var requestsSecondsProperties = spyOpensearchMetricsProperties.getRequestsSeconds();

        doTestRegisterRequestSecondsMetricWithVerificationsOnSpyMetricBuilder(spyTimerBuilder -> {
            Mockito.verify(spyTimerBuilder).publishPercentiles(
                listToArray(requestsSecondsProperties.getQuantiles())
            );
            Mockito.verify(spyTimerBuilder).publishPercentileHistogram(
                requestsSecondsProperties.getQuantileHistogram()
            );
            Mockito.verify(spyTimerBuilder, Mockito.never()).serviceLevelObjectives(
                ArgumentMatchers.any(Duration[].class)
            );
        });
    }

    protected void doTestRegisterRequestSecondsMetricWithVerificationsOnSpyMetricBuilder(
        Consumer<Timer.Builder> doAdditionalVerificationsOnSpy
    ) throws MetricsRegistrationException {

        var spyTimerBuilder = Mockito.spy(
            Timer.builder(OpensearchMetricsProvider.REQUESTS_SECONDS_METRIC_NAME)
        );

        try (var mockStatic = Mockito.mockStatic(Timer.class)) {
            mockStatic.when(
                () -> Timer.builder(OpensearchMetricsProvider.REQUESTS_SECONDS_METRIC_NAME)
            ).thenReturn(spyTimerBuilder);

            opensearchMetricsProvider.registerMetrics(mockDatabaseMetricProperties);

            var requestsSecondsProperties = spyOpensearchMetricsProperties.getRequestsSeconds();

            Mockito.verify(spyTimerBuilder).description(
                OpensearchMetricsProvider.REQUESTS_SECONDS_METRIC_DESCRIPTION
            );
            Mockito.verify(spyTimerBuilder).minimumExpectedValue(
                requestsSecondsProperties.getMinimumExpectedValue()
            );
            Mockito.verify(spyTimerBuilder).maximumExpectedValue(
                requestsSecondsProperties.getMaximumExpectedValue()
            );

            doAdditionalVerificationsOnSpy.accept(spyTimerBuilder);

            Mockito.verify(mockHttpClientBuilder).addExecInterceptorLast(
                ArgumentMatchers.eq(OpensearchMetricsProvider.MICROMETER_EXEC_INTERCEPTOR_NAME),
                ArgumentMatchers.any(ObservationExecChainHandler.class)
            );
        }
    }

    protected double[] listToArray(List<Double> list) {
        return list.stream()
            .mapToDouble(Double::doubleValue)
            .toArray();
    }
}
