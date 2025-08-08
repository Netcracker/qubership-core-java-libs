package com.netcracker.cloud.dbaas.client.config;

import org.qubership.cloud.dbaas.client.cassandra.migration.model.settings.SchemaMigrationSettings;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnableDbaasCassandra
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfig.class,
        initializers = ConfigDataApplicationContextInitializer.class)
@ActiveProfiles("migration")
public class DbaasCassandraMigrationConfigurationTest {

    @Autowired
    SchemaMigrationSettings schemaMigrationSettings;

    @Test
    void testMigrationPropertiesLoaded() {
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
}
