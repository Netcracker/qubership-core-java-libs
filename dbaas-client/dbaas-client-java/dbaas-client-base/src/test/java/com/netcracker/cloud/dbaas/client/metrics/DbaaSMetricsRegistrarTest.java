package com.netcracker.cloud.dbaas.client.metrics;

import com.netcracker.cloud.dbaas.client.entity.test.TestDBType;
import com.netcracker.cloud.dbaas.client.exceptions.MetricsRegistrationException;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.netcracker.cloud.dbaas.client.DbaasConst.SCOPE;
import static com.netcracker.cloud.dbaas.client.DbaasConst.SERVICE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DbaaSMetricsRegistrarTest {

    private static String TEST_DB_NAME = "test-database";
    private static String TEST_DB_ROLE = "test-role";

    @Test
    void testMetricsProviderIsCalled() throws MetricsRegistrationException {
        MetricsProvider metricsProvider = mock(MetricsProvider.class);
        when(metricsProvider.getSupportedDatabaseType()).thenReturn(TestDBType.INSTANCE.getDatabaseClass());

        DbaaSMetricsRegistrar metricsRegistrar = new DbaaSMetricsRegistrar(List.of(metricsProvider));
        DatabaseMetricProperties metricProperties = getTestDatabaseMetricProperties();
        metricsRegistrar.registerMetrics(TestDBType.INSTANCE, metricProperties);
        verify(metricsProvider, times(1)).registerMetrics(metricProperties);
    }

    @Test
    void testMetricsRegistrationExceptionIsNotThrown() throws MetricsRegistrationException {
        MetricsProvider metricsProvider = mock(MetricsProvider.class);
        when(metricsProvider.getSupportedDatabaseType()).thenReturn(TestDBType.INSTANCE.getDatabaseClass());
        doThrow(new MetricsRegistrationException("Failed to register metrics")).when(metricsProvider).registerMetrics(any());

        DbaaSMetricsRegistrar metricsRegistrar = new DbaaSMetricsRegistrar(List.of(metricsProvider));
        DatabaseMetricProperties metricProperties = getTestDatabaseMetricProperties();
        assertDoesNotThrow(() -> {
            metricsRegistrar.registerMetrics(TestDBType.INSTANCE, metricProperties);
        });
        verify(metricsProvider, times(1)).registerMetrics(metricProperties);
    }

    private DatabaseMetricProperties getTestDatabaseMetricProperties() {
        SortedMap<String, Object> classifier = new TreeMap<>();
        classifier.put(SCOPE, SERVICE);
        DatabaseMetricProperties metricProperties = new DatabaseMetricProperties(TEST_DB_NAME, TEST_DB_ROLE, classifier, Collections.emptyMap(), Collections.emptyMap());
        return metricProperties;
    }
}
