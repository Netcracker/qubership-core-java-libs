package com.netcracker.cloud.quarkus.dbaas.cassandraclient.config.properties.migration;

import com.netcracker.cloud.dbaas.client.cassandra.migration.model.settings.SchemaMigrationSettings;

import java.util.Optional;

public interface MigrationProperties {
    /**
     * Whether to enable migration.
     */
    Optional<Boolean> enabled();

    /**
     * Name of the table to store schema version history
     */
    Optional<String> schemaHistoryTableName();

    /**
     * Version properties
     */
    VersionProperties version();

    /**
     * Template properties
     */
    TemplateProperties template();

    /**
     * Migration lock properties
     */
    LockProperties lock();

    /**
     * Schema Agreement properties
     */
    SchemaAgreementProperties schemaAgreement();

    /**
     * Amazon Keyspaces related properties
     */
    AmazonKeyspacesProperties amazonKeyspaces();

    default SchemaMigrationSettings toSchemaMigrationSettings() {
        SchemaMigrationSettings.SchemaMigrationSettingsBuilder builder = SchemaMigrationSettings.builder();
        enabled().ifPresent(builder::enabled);
        schemaHistoryTableName().ifPresent(builder::withSchemaHistoryTableName);
        builder.withVersionSettings(version().toVersionSettings());
        builder.withTemplateSettings(template().toTemplateSettings());
        builder.withLockSettings(lock().toLockSettings());
        builder.withSchemaAgreement(schemaAgreement().toSchemaAgreementSettings());
        builder.withAmazonKeyspacesSettings(amazonKeyspaces().toAmazonKeyspacesSettings());
        return builder.build();
    }
}
