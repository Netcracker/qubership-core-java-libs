package com.netcracker.cloud.core.quarkus.dbaas.datasource.metrics;

import com.netcracker.cloud.dbaas.client.exceptions.MetricsRegistrationException;
import com.netcracker.cloud.dbaas.client.metrics.DatabaseMetricProperties;
import io.agroal.api.AgroalDataSource;
import io.agroal.api.AgroalDataSourceMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Stream;

import static com.netcracker.cloud.core.quarkus.dbaas.datasource.metrics.PostgresMicrometerMetricsProvider.DATASOURCE_PARAMETER;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Uses the registry that Quarkus creates in PrometheusMeterRegistryProducer: the simpleclient one with
 * throwExceptionOnRegistrationFailure.
 */
class PostgresMicrometerMetricsProviderTest {

    private static final int METER_COUNT = 16;
    private static final String AGROAL_PREFIX = "agroal.";

    private final PostgresMicrometerMetricsProvider provider = new PostgresMicrometerMetricsProvider();
    private List<MeterRegistry> otherRegistries;
    private PrometheusMeterRegistry prometheus;

    @BeforeEach
    void setUp() {
        prometheus = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        prometheus.throwExceptionOnRegistrationFailure();
        Metrics.addRegistry(prometheus);
        otherRegistries = Stream.<MeterRegistry>generate(SimpleMeterRegistry::new).limit(5).toList();
        otherRegistries.forEach(Metrics::addRegistry);
    }

    @AfterEach
    void tearDown() {
        Metrics.removeRegistry(prometheus);
        otherRegistries.forEach(Metrics::removeRegistry);
        Metrics.globalRegistry.clear();
        prometheus.close();
        otherRegistries.forEach(MeterRegistry::close);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void onlyPrometheusLosesMetersOfSecondKindOfDatabase(boolean serviceFirst) throws MetricsRegistrationException {
        DatabaseMetricProperties service = properties("service_db", Map.of("scope", "service"));
        DatabaseMetricProperties tenant = properties("tenant_db", Map.of("scope", "tenant", "tenantId", "tenant_a"));

        provider.registerMetrics(serviceFirst ? service : tenant);
        provider.registerMetrics(serviceFirst ? tenant : service);

        String prometheusDatabase = serviceFirst ? "service_db" : "tenant_db";
        assertEquals(METER_COUNT, agroalMeterCount(prometheus));
        assertEquals(METER_COUNT, prometheus.getMeters().stream()
                .filter(meter -> prometheusDatabase.equals(meter.getId().getTag("name")))
                .count());
        otherRegistries.forEach(registry -> assertEquals(METER_COUNT * 2, agroalMeterCount(registry)));
        assertEquals(METER_COUNT * 2, agroalMeterCount(Metrics.globalRegistry));
    }

    private static long agroalMeterCount(MeterRegistry registry) {
        return registry.getMeters().stream().filter(meter -> meter.getId().getName().startsWith(AGROAL_PREFIX)).count();
    }

    private static DatabaseMetricProperties properties(String name, Map<String, Object> classifier) {
        AgroalDataSource dataSource = Mockito.mock(AgroalDataSource.class);
        Mockito.when(dataSource.getMetrics()).thenReturn(Mockito.mock(AgroalDataSourceMetrics.class));
        SortedMap<String, Object> sortedClassifier = new TreeMap<>(classifier);
        return DatabaseMetricProperties.builder()
                .databaseName(name)
                .role("admin")
                .classifier(sortedClassifier)
                .extraParameters(Map.of(DATASOURCE_PARAMETER, dataSource))
                .build();
    }
}
