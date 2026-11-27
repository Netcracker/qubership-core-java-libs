package com.netcracker.cloud.dbaas.client.opensearch.metrics;

import com.netcracker.cloud.dbaas.client.exceptions.MetricsRegistrationException;
import com.netcracker.cloud.dbaas.client.metrics.DatabaseMetricProperties;
import com.netcracker.cloud.dbaas.client.opensearch.entity.DbaasOpensearchMetricsProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.httpcomponents.hc5.ApacheHttpClientContext;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import static com.netcracker.cloud.dbaas.client.opensearch.metrics.OpensearchMetricsProvider.HTTP_CLIENT_BUILDER;
import static com.netcracker.cloud.dbaas.client.opensearch.metrics.OpensearchMetricsProvider.REQUESTS_SECONDS_METRIC_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Uses the registry that Quarkus creates in PrometheusMeterRegistryProducer: the simpleclient one with
 * throwExceptionOnRegistrationFailure.
 */
class OpensearchConflictingMetricTagsTest {

    private static final String SCOPE_TAG = "cl_scope";

    @Test
    void onlyPrometheusLosesTimerOfSecondKindOfDatabase() throws MetricsRegistrationException {
        PrometheusMeterRegistry prometheus = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        prometheus.throwExceptionOnRegistrationFailure();
        List<MeterRegistry> others = Stream.<MeterRegistry>generate(SimpleMeterRegistry::new).limit(5).toList();
        CompositeMeterRegistry composite = new CompositeMeterRegistry();
        composite.add(prometheus);
        others.forEach(composite::add);
        OpensearchMetricsProvider provider = new OpensearchMetricsProvider(composite, new DbaasOpensearchMetricsProperties());

        recordRequest(provider, composite, properties(Map.of("scope", "service")));
        recordRequest(provider, composite, properties(Map.of("scope", "tenant", "tenantId", "tenant_a")));

        assertNotNull(prometheus.find(REQUESTS_SECONDS_METRIC_NAME).tag(SCOPE_TAG, "service").timer());
        assertNull(prometheus.find(REQUESTS_SECONDS_METRIC_NAME).tag(SCOPE_TAG, "tenant").timer());
        for (MeterRegistry other : others) {
            assertEquals(1, other.find(REQUESTS_SECONDS_METRIC_NAME).tag(SCOPE_TAG, "service").timer().count());
            assertEquals(1, other.find(REQUESTS_SECONDS_METRIC_NAME).tag(SCOPE_TAG, "tenant").timer().count());
        }
    }

    private static void recordRequest(OpensearchMetricsProvider provider, MeterRegistry registry,
                                      DatabaseMetricProperties properties) throws MetricsRegistrationException {
        provider.registerMetrics(properties);
        ApacheHttpClientContext context = Mockito.mock(ApacheHttpClientContext.class);
        Mockito.when(context.get(OpensearchClientRequestsSecondsObservationHandler.START_TIME_IN_MILLIS))
                .thenReturn(System.currentTimeMillis());
        new OpensearchClientRequestsSecondsObservationHandler(properties.getMetricTags(), registry, provider.getRequestsSecondsBaseMetricBuilder())
                .onStop(context);
    }

    private static DatabaseMetricProperties properties(Map<String, Object> classifier) {
        return DatabaseMetricProperties.builder()
                .role("admin")
                .classifier(new TreeMap<>(classifier))
                .extraParameters(Map.of(HTTP_CLIENT_BUILDER, Mockito.mock(HttpAsyncClientBuilder.class)))
                .build();
    }
}
