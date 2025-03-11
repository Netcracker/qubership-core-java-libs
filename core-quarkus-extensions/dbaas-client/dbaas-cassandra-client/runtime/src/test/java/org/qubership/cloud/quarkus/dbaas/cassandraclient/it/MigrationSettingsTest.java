package org.qubership.cloud.quarkus.dbaas.cassandraclient.it;

import org.qubership.cloud.dbaas.client.cassandra.migration.model.settings.SchemaMigrationSettings;
import org.qubership.cloud.quarkus.dbaas.cassandraclient.config.properties.CassandraProperties;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import lombok.NoArgsConstructor;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@TestProfile(MigrationSettingsTest.MigrationSettingsProfile.class)
class MigrationSettingsTest {

    @Inject
    CassandraProperties cassandraProperties;

    @Test
    void testMigrationConfigurationLoaded() {
        SchemaMigrationSettings schemaMigrationSettings = cassandraProperties.getCassandraSessionProperties().getMigration().toSchemaMigrationSettings();
        assertTrue(schemaMigrationSettings.enabled());
        assertTrue(schemaMigrationSettings.amazonKeyspaces().enabled());
        assertEquals(111, schemaMigrationSettings.amazonKeyspaces().tableStatusCheck().preDelay());
        assertEquals(111, schemaMigrationSettings.amazonKeyspaces().tableStatusCheck().retryDelay());
        assertEquals("schema-history-table-test", schemaMigrationSettings.schemaHistoryTableName());
        assertEquals("version-dir-path-test", schemaMigrationSettings.version().directoryPath());
        assertEquals("settings-res-path-test", schemaMigrationSettings.version().settingsResourcePath());
        assertEquals("res-name=pattern-test", schemaMigrationSettings.version().resourceNamePattern());
        assertEquals("def-res-path-test", schemaMigrationSettings.template().definitionsResourcePath());
        assertEquals("lock-table-name-test", schemaMigrationSettings.lock().tableName());
        assertEquals(111, schemaMigrationSettings.lock().retryDelay());
        assertEquals(111, schemaMigrationSettings.lock().lockLifetime());
        assertEquals(111, schemaMigrationSettings.lock().extensionPeriod());
        assertEquals(111, schemaMigrationSettings.lock().extensionFailRetryDelay());
        assertEquals(111, schemaMigrationSettings.schemaAgreement().awaitRetryDelay());
    }

    @NoArgsConstructor
    protected static final class MigrationSettingsProfile extends CassandraResourceProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            Map<String, String> properties = new HashMap<>();
            properties.put("quarkus.dbaas.cassandra.migration.enabled", "true");
            properties.put("quarkus.dbaas.cassandra.migration.amazon-keyspaces.enabled", "true");
            properties.put("quarkus.dbaas.cassandra.migration.amazon-keyspaces.table-status-check.pre-delay", "111");
            properties.put("quarkus.dbaas.cassandra.migration.amazon-keyspaces.table-status-check.retry-delay", "111");
            properties.put("quarkus.dbaas.cassandra.migration.schema-history-table-name", "schema-history-table-test");
            properties.put("quarkus.dbaas.cassandra.migration.version.directory-path", "version-dir-path-test");
            properties.put("quarkus.dbaas.cassandra.migration.version.settings-resource-path", "settings-res-path-test");
            properties.put("quarkus.dbaas.cassandra.migration.version.resource-name-pattern", "res-name=pattern-test");
            properties.put("quarkus.dbaas.cassandra.migration.template.definitions-resource-path", "def-res-path-test");
            properties.put("quarkus.dbaas.cassandra.migration.lock.table-name", "lock-table-name-test");
            properties.put("quarkus.dbaas.cassandra.migration.lock.retry-delay", "111");
            properties.put("quarkus.dbaas.cassandra.migration.lock.lock-lifetime", "111");
            properties.put("quarkus.dbaas.cassandra.migration.lock.extension-period", "111");
            properties.put("quarkus.dbaas.cassandra.migration.lock.extension-fail-retry-delay", "111");
            properties.put("quarkus.dbaas.cassandra.migration.schema-agreement.await-retry-delay", "111");
            return properties;
        }
    }
}
