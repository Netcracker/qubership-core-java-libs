package com.netcracker.cloud.dbaas.client.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Uses the registry that Quarkus creates in PrometheusMeterRegistryProducer: the simpleclient one with
 * throwExceptionOnRegistrationFailure.
 */
class DbaasMetricTagsFilterTest {

    private static final String METER = "agroal.active.count";
    private static final String SCOPE_TAG = "cl_scope";

    private PrometheusMeterRegistry prometheus;

    @BeforeEach
    void setUp() {
        prometheus = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        prometheus.throwExceptionOnRegistrationFailure();
        // On a service the filter is added when the registry already has meters
        prometheus.counter("registered.before.filter");
    }

    @AfterEach
    void tearDown() {
        prometheus.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"agroal.active.count", "cassandra.session.cql-requests", "opensearch_client_requests_seconds"})
    void conflictingMeterIsDenied(String name) {
        DbaasMetricTagsFilter.applyTo(prometheus);

        registerService(prometheus, name);
        assertDoesNotThrow(() -> registerTenant(prometheus, name, "tenant_a"));

        assertNotNull(prometheus.find(name).tag(SCOPE_TAG, "service").counter());
        assertNull(prometheus.find(name).tag(SCOPE_TAG, "tenant").counter());
    }

    @Test
    void sameTagKeysAreKept() {
        DbaasMetricTagsFilter.applyTo(prometheus);

        registerTenant(prometheus, METER, "tenant_a");
        registerTenant(prometheus, METER, "tenant_b");

        assertNotNull(prometheus.find(METER).tag("cl_tenantId", "tenant_a").counter());
        assertNotNull(prometheus.find(METER).tag("cl_tenantId", "tenant_b").counter());
    }

    @Test
    void tagKeysAreFreeAfterMetersAreRemoved() {
        DbaasMetricTagsFilter.applyTo(prometheus);

        prometheus.remove(registerService(prometheus, METER));
        registerTenant(prometheus, METER, "tenant_a");

        assertNotNull(prometheus.find(METER).tag(SCOPE_TAG, "tenant").counter());
    }

    // Composite registries keep children in an identity set, so several registries end up on both sides of Prometheus
    @Test
    void otherRegistriesOfCompositeGetConflictingMeter() {
        List<MeterRegistry> others = Stream.<MeterRegistry>generate(SimpleMeterRegistry::new).limit(5).toList();
        CompositeMeterRegistry composite = new CompositeMeterRegistry();
        composite.add(prometheus);
        others.forEach(composite::add);
        DbaasMetricTagsFilter.applyTo(composite);

        registerService(composite, METER);
        registerTenant(composite, METER, "tenant_a").increment();

        others.forEach(other -> assertNotNull(other.find(METER).tag(SCOPE_TAG, "service").counter()));
        others.forEach(other -> assertNotNull(other.find(METER).tag(SCOPE_TAG, "tenant").counter()));
        assertNull(prometheus.find(METER).tag(SCOPE_TAG, "tenant").counter());
    }

    // CompositeMeterRegistry.add syncs existing meters onto the new child; without the filter that throws on Prometheus
    @Test
    void addingPrometheusToCompositeWithConflictingMetersDoesNotFail() {
        CompositeMeterRegistry composite = new CompositeMeterRegistry();
        registerService(composite, METER);
        registerTenant(composite, METER, "tenant_a");

        DbaasMetricTagsFilter.applyTo(prometheus);
        assertDoesNotThrow(() -> composite.add(prometheus));
        assertNotNull(prometheus.find(METER).tag(SCOPE_TAG, "service").counter());
        assertNull(prometheus.find(METER).tag(SCOPE_TAG, "tenant").counter());
    }

    @Test
    void registryWithoutTagKeysCheckIsNotFiltered() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        DbaasMetricTagsFilter.applyTo(registry);

        registerService(registry, METER);
        registerTenant(registry, METER, "tenant_a");

        assertNotNull(registry.find(METER).tag(SCOPE_TAG, "tenant").counter());
    }

    @Test
    void otherMetersAreNotFiltered() {
        DbaasMetricTagsFilter.applyTo(prometheus);

        registerService(prometheus, "other.meter");

        assertThrows(IllegalArgumentException.class, () -> registerTenant(prometheus, "other.meter", "tenant_a"));
    }

    // This is why DbaaSMetricsRegistrar still catches IllegalArgumentException
    @Test
    void earlierFilterAcceptingMeterBypassesThisOne() {
        prometheus.config().meterFilter(MeterFilter.accept(id -> id.getName().equals(METER)));
        DbaasMetricTagsFilter.applyTo(prometheus);

        registerService(prometheus, METER);

        assertThrows(IllegalArgumentException.class, () -> registerTenant(prometheus, METER, "tenant_a"));
    }

    private static Counter registerService(MeterRegistry registry, String name) {
        return registry.counter(name, SCOPE_TAG, "service");
    }

    private static Counter registerTenant(MeterRegistry registry, String name, String tenantId) {
        return registry.counter(name, SCOPE_TAG, "tenant", "cl_tenantId", tenantId);
    }
}
