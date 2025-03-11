package org.qubership.cloud.core.quarkus.dbaas.datasource.testcontainers;

import org.qubership.cloud.core.quarkus.dbaas.datasource.config.flyway.CoreFlywayConfig;
import org.qubership.cloud.core.quarkus.dbaas.datasource.config.flyway.FlywayConfig;
import org.qubership.cloud.core.quarkus.dbaas.datasource.service.DbaaSPostgresDbCreationService;
import org.qubership.cloud.core.quarkus.dbaas.datasource.testcontainers.configuration.PostgresqlContainerResource;
import org.qubership.cloud.dbaas.client.entity.database.PostgresDatabase;
import org.qubership.cloud.dbaas.client.management.DbaasDbClassifier;
import io.quarkus.flyway.runtime.FlywayRuntimeConfig;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.qubership.cloud.core.quarkus.dbaas.datasource.CommonTestUtils.TEST_NAMESPACE;
import static org.qubership.cloud.core.quarkus.dbaas.datasource.CommonTestUtils.getServiceClassifier;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


@QuarkusTestResource(PostgresqlContainerResource.class)
@TestProfile(DbaaSPostgresDbCreationFlywayCleanTest.CleanAndMigrateAtStartTestProfile.class)
@QuarkusTest
@Slf4j
class DbaaSPostgresDbCreationFlywayCleanTest {

    private static final String CLEAN_AND_MIGRATE_AT_START_PROPERTY = "quarkus.dbaas.flyway.clean-and-migrate-at-start";
    private static final String CLEAN_DISABLED_PROPERTY = "quarkus.dbaas.flyway.clean-disabled";
    private static final String DATASOURCES_LOCATIONS = "db/configs/folder";

    @Inject
    DbaaSPostgresDbCreationService dbaaSPostgresDbCreationService;

    @Inject
    CoreFlywayConfig coreFlywayConfig;

    @Inject
    FlywayRuntimeConfig flywayRuntimeConfig;

    FluentConfiguration defaultFlywayConfiguration = new FluentConfiguration();

    @Test
    void testFlywayCleanConfiguration() {
        assertTrue(coreFlywayConfig.globalFlywayConfig.cleanAndMigrateAtStart);
        assertFalse(coreFlywayConfig.globalFlywayConfig.cleanDisabled);
        assertFalse(flywayRuntimeConfig.defaultDataSource.cleanDisabled);
        assertTrue(defaultFlywayConfiguration.isCleanDisabled());
        Map<String, FlywayConfig> logicalDbFlywayConfigMap = coreFlywayConfig.datasources;
        assertEquals(1, logicalDbFlywayConfigMap.size());
        assertTrue(logicalDbFlywayConfigMap.containsKey("configs"));
        FlywayConfig config = logicalDbFlywayConfigMap.get("configs");
        assertTrue(config.location.isPresent());
        assertEquals(DATASOURCES_LOCATIONS, config.location.get());
    }

    @Test
    void mustCleanAndMigrateAtStart() {
        DbaasDbClassifier classifier = getServiceClassifier();
        PostgresDatabase createdDb = dbaaSPostgresDbCreationService.getOrCreatePostgresDatabase(classifier);
        assertNotNull(createdDb);
    }

    @NoArgsConstructor
    protected static final class CleanAndMigrateAtStartTestProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            Map<String, String> properties = new HashMap<>();
            properties.put("cloud.microservice.name", "dbaas-client-postgres-flyway-test");
            properties.put("cloud.microservice.namespace", TEST_NAMESPACE);
            properties.put(CLEAN_AND_MIGRATE_AT_START_PROPERTY, "true");
            properties.put("quarkus.dbaas.flyway.datasources.configs.location", DATASOURCES_LOCATIONS);
            return properties;
        }

    }

}
