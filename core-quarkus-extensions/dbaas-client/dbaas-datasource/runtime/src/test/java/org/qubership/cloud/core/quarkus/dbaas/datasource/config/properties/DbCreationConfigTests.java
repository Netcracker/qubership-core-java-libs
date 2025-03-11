package org.qubership.cloud.core.quarkus.dbaas.datasource.config.properties;

import org.qubership.cloud.dbaas.client.entity.settings.PostgresSettings;
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
import java.util.Optional;

import static org.qubership.cloud.core.quarkus.dbaas.datasource.CommonTestUtils.TEST_NAMESPACE;


public class DbCreationConfigTests {

    public static final String DB_SETTINGS_PROPERTY_KEY = "quarkus.dbaas.postgresql.api.service.database-settings";
    public static final String ALL_TENANTS_DB_SETTINGS_PROPERTY_KEY = "quarkus.dbaas.postgresql.api.tenant.database-settings";
    public static final String DB_RUNTIME_USER_PROPERTY_KEY = "quarkus.dbaas.postgresql.api.runtime-user-role";

    public static final String DB_NAME_PREFIX = "quarkus.dbaas.postgresql.api.db-prefix";

    @QuarkusTest
    @TestProfile(ExactOneDbSettingsPropertyTestProfile.class)
    public static class WithDbSettingsTest {
        @Inject
        DbaaSPostgresDbCreationConfig config;

        @Test
        public void testPgExtensionsContainsOneElem() {
            Optional<PostgresSettings> settingsOpt = config.serviceDbConfiguration.databaseSettings;
            Assertions.assertTrue(settingsOpt.isPresent());

            PostgresSettings settings = settingsOpt.get();
            List<String> pgExtensions = settings.getPgExtensions();

            Assertions.assertNotNull(pgExtensions);
            Assertions.assertEquals(1, pgExtensions.size());
            Assertions.assertEquals(DbCreationConfigTests.ExactOneDbSettingsPropertyTestProfile.PROPERTY_VAL,
                    pgExtensions.get(0));
        }
    }

    @QuarkusTest
    @TestProfile(AllTenantsDbSettingsPropertyTestProfile.class)
    public static class AllTenantsWithDbSettingsTest {
        @Inject
        DbaaSPostgresDbCreationConfig config;

        @Test
        public void testPgExtensionsContainsOneElemForAllTenants() {
            Optional<PostgresSettings> settingsOpt = config.allTenantsDbConfiguration.databaseSettings;
            Optional<PostgresSettings> settingsOptFromGet = config.getPostgresDbConfiguration("some-tenant").databaseSettings;
            Assertions.assertEquals(settingsOpt, settingsOptFromGet);
            Assertions.assertTrue(settingsOpt.isPresent());

            PostgresSettings settings = settingsOpt.get();
            List<String> pgExtensions = settings.getPgExtensions();

            Assertions.assertNotNull(pgExtensions);
            Assertions.assertEquals(1, pgExtensions.size());
            Assertions.assertEquals(DbCreationConfigTests.ExactOneDbSettingsPropertyTestProfile.PROPERTY_VAL,
                    pgExtensions.get(0));
        }
    }

    @QuarkusTest
    @TestProfile(ExactDbaasApiPropertiesTestProfile.class)
    public static class WithDbRuntimeUserTest {
        @Inject
        DbaaSPostgresDbCreationConfig config;

        @Test
        public void testPgExtensionsContainsRoleAndPrefix() {
            Optional<String> userRole = config.dbaasApiPropertiesConfig.runtimeUserRole;
            Optional<String> dbPrefix = config.dbaasApiPropertiesConfig.dbPrefix;
            Assertions.assertTrue(userRole.isPresent());
            Assertions.assertEquals("admin", userRole.get());
            Assertions.assertNotNull(dbPrefix);
            Assertions.assertEquals("test-prefix", dbPrefix.get());
        }
    }

    @QuarkusTest
    @TestProfile(MultiplePgExtensionsPropertiesTestProfile.class)
    public static class MultiplePgExtensionsTest {
        @Inject
        DbaaSPostgresDbCreationConfig config;

        @Test
        public void testPgExtensionsContains2Elem() {
            Optional<PostgresSettings> settingsOpt = config.serviceDbConfiguration.databaseSettings;
            Assertions.assertTrue(settingsOpt.isPresent());

            PostgresSettings settings = settingsOpt.get();
            List<String> pgExtensions = settings.getPgExtensions();

            Assertions.assertNotNull(pgExtensions);
            Assertions.assertEquals(2, pgExtensions.size());

            Assertions.assertTrue(pgExtensions.contains(MultiplePgExtensionsPropertiesTestProfile.FIRST_PROPERTY_VAL));
            Assertions.assertTrue(pgExtensions.contains(MultiplePgExtensionsPropertiesTestProfile.SECOND_PROPERTY_VALl));
        }
    }

    @QuarkusTest
    @TestProfile(WithoutDbSettingsPropertyTestProfile.class)
    public static class WithoutDbSettingsTest {
        @Inject
        DbaaSPostgresDbCreationConfig config;

        @Test
        public void testPostgresSettingsMustBeNull() {
            Optional<PostgresSettings> settingsOpt = config.serviceDbConfiguration.databaseSettings;
            Assertions.assertFalse(settingsOpt.isPresent());
        }
    }

    private static Map<String, String> getBaseProperties() {
        Map<String, String> properties = new HashMap<>();

        properties.put("quarkus.datasource.devservices", "false");
        properties.put("cloud.microservice.name", "dbaas-client-postgres-params-test");
        properties.put("cloud.microservice.namespace", TEST_NAMESPACE);
        properties.put("quarkus.http.test-port", "0");
        properties.put("quarkus.http.test-ssl-port", "0");

        return properties;
    }

    @NoArgsConstructor
    protected static final class WithoutDbSettingsPropertyTestProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return getBaseProperties();
        }
        @Override
        public boolean disableGlobalTestResources() {
            return true;
        }
    }

    @NoArgsConstructor
    protected static final class ExactOneDbSettingsPropertyTestProfile implements QuarkusTestProfile {
        protected static final String PROPERTY_VAL = "pgcrypto";

        @Override
        public Map<String, String> getConfigOverrides() {
            Map<String, String> properties = getBaseProperties();
            properties.put(DB_SETTINGS_PROPERTY_KEY, PROPERTY_VAL);

            return properties;
        }
        @Override
        public boolean disableGlobalTestResources() {
            return true;
        }
    }

    @NoArgsConstructor
    protected static final class AllTenantsDbSettingsPropertyTestProfile implements QuarkusTestProfile {
        protected static final String PROPERTY_VAL = "pgcrypto";

        @Override
        public Map<String, String> getConfigOverrides() {
            Map<String, String> properties = getBaseProperties();
            properties.put(ALL_TENANTS_DB_SETTINGS_PROPERTY_KEY, PROPERTY_VAL);

            return properties;
        }
        @Override
        public boolean disableGlobalTestResources() {
            return true;
        }
    }

    @NoArgsConstructor
    protected static final class ExactDbaasApiPropertiesTestProfile implements QuarkusTestProfile {
        protected static final String ROLE_PROPERTY_VAL = "admin";

        protected static final String DB_PREFIX_PROPERTY_VAL = "test-prefix";

        @Override
        public Map<String, String> getConfigOverrides() {
            Map<String, String> properties = getBaseProperties();
            properties.put(DB_RUNTIME_USER_PROPERTY_KEY, ROLE_PROPERTY_VAL);
            properties.put(DB_NAME_PREFIX, DB_PREFIX_PROPERTY_VAL);

            return properties;
        }
        @Override
        public boolean disableGlobalTestResources() {
            return true;
        }
    }

    @NoArgsConstructor
    protected static final class MultiplePgExtensionsPropertiesTestProfile implements QuarkusTestProfile {
        protected static final String FIRST_PROPERTY_VAL = "pgcrypto";
        protected static final String SECOND_PROPERTY_VALl = "anotherPgExt";

        @Override
        public Map<String, String> getConfigOverrides() {
            Map<String, String> properties = getBaseProperties();
            properties.put(DB_SETTINGS_PROPERTY_KEY, FIRST_PROPERTY_VAL.concat(",").concat(SECOND_PROPERTY_VALl));
            return properties;
        }
        @Override
        public boolean disableGlobalTestResources() {
            return true;
        }
    }
}
