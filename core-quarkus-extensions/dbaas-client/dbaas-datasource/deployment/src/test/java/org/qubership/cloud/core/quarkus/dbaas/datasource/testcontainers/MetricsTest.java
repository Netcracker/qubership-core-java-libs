package org.qubership.cloud.core.quarkus.dbaas.datasource.testcontainers;

import org.qubership.cloud.framework.contexts.tenant.context.TenantContext;
import org.qubership.cloud.core.quarkus.dbaas.datasource.service.DbaaSPostgresDbCreationService;
import org.qubership.cloud.core.quarkus.dbaas.datasource.testcontainers.configuration.PostgresqlContainerResource;
import org.qubership.cloud.dbaas.client.entity.database.PostgresDatabase;
import org.qubership.cloud.dbaas.client.management.DbaasDbClassifier;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import lombok.NoArgsConstructor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.qubership.cloud.core.quarkus.dbaas.datasource.testcontainers.configuration.TestUtils.FIRST_TENANT_ID;
import static org.qubership.cloud.core.quarkus.dbaas.datasource.testcontainers.configuration.TestUtils.MICROSERVICE_CLASSIFIER_BUILDER;
import static org.qubership.cloud.core.quarkus.dbaas.datasource.testcontainers.configuration.TestUtils.SECOND_TENANT_ID;
import static org.qubership.cloud.core.quarkus.dbaas.datasource.testcontainers.configuration.TestUtils.TENANT_CLASSIFIER_BUILDER;
import static org.qubership.cloud.dbaas.client.DbaasConst.SCOPE;
import static org.qubership.cloud.dbaas.client.DbaasConst.SERVICE;
import static org.qubership.cloud.dbaas.client.DbaasConst.TENANT;
import static org.qubership.cloud.dbaas.client.DbaasConst.TENANT_ID;
import static org.qubership.cloud.dbaas.client.metrics.DatabaseMetricProperties.CLASSIFIER_TAG_PREFIX;

@QuarkusTestResource(PostgresqlContainerResource.class)
@TestProfile(MetricsTest.MetricsEnabledTestProfile.class)
@QuarkusTest
class MetricsTest {

    @Inject
    DbaaSPostgresDbCreationService creationService;

    @Test
    void testMetricsRegisteredForDatasources() {
        DbaasDbClassifier serviceClassifier = MICROSERVICE_CLASSIFIER_BUILDER.build();

        TenantContext.set(FIRST_TENANT_ID);
        DbaasDbClassifier firstTenantClassifier = TENANT_CLASSIFIER_BUILDER.build();

        TenantContext.set(SECOND_TENANT_ID);
        DbaasDbClassifier secondTenantClassifier = TENANT_CLASSIFIER_BUILDER.build();

        Assertions.assertNotEquals(serviceClassifier, firstTenantClassifier);
        Assertions.assertNotEquals(serviceClassifier, secondTenantClassifier);
        Assertions.assertNotEquals(firstTenantClassifier, secondTenantClassifier);

        PostgresDatabase serviceDb = creationService.getOrCreatePostgresDatabase(serviceClassifier);
        PostgresDatabase firstTenantDb = creationService.getOrCreatePostgresDatabase(firstTenantClassifier);
        PostgresDatabase secondTenantDb = creationService.getOrCreatePostgresDatabase(secondTenantClassifier);

        MeterRegistry meterRegistry = Metrics.globalRegistry;
        List<Meter> meters = meterRegistry.getMeters();
        Assertions.assertTrue(meters.stream().anyMatch(meter -> serviceDb.getName().equals(meter.getId().getTag("name"))
                && meter.getId().getTag(CLASSIFIER_TAG_PREFIX + SCOPE).equals(SERVICE)));

        Assertions.assertTrue(meters.stream().anyMatch(meter -> firstTenantDb.getName().equals(meter.getId().getTag("name"))
                && meter.getId().getTag(CLASSIFIER_TAG_PREFIX + SCOPE).equals(TENANT)
                && meter.getId().getTag(CLASSIFIER_TAG_PREFIX + TENANT_ID).equals(FIRST_TENANT_ID)));

        Assertions.assertTrue(meters.stream().anyMatch(meter -> secondTenantDb.getName().equals(meter.getId().getTag("name"))
                && meter.getId().getTag(CLASSIFIER_TAG_PREFIX + SCOPE).equals(TENANT)
                && meter.getId().getTag(CLASSIFIER_TAG_PREFIX + TENANT_ID).equals(SECOND_TENANT_ID)));
    }

    @NoArgsConstructor
    protected static final class MetricsEnabledTestProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            Map<String, String> properties = new HashMap<>();
            properties.put("quarkus.micrometer.enabled", "true");
            return properties;
        }

    }

}
