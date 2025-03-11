package org.qubership.cloud.quarkus.dbaas.cassandraclient.config.properties.migration;

import org.qubership.cloud.dbaas.client.cassandra.migration.model.settings.SchemaMigrationSettings;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Optional;

@ConfigGroup
@Getter
@Accessors(fluent = true)
public class MigrationProperties {
    /**
     * Whether to enable migration.
     */
    @ConfigItem
    Optional<Boolean> enabled;

    /**
     * Name of the table to store schema version history
     */
    @ConfigItem
    Optional<String> schemaHistoryTableName;

    /**
     * Version properties
     */
    @ConfigItem
    VersionProperties version;

    /**
     * Template properties
     */
    @ConfigItem
    TemplateProperties template;

    /**
     * Migration lock properties
     */
    @ConfigItem
    LockProperties lock;

    /**
     * Schema Agreement properties
     */
    @ConfigItem
    SchemaAgreementProperties schemaAgreement;

    /**
     * Amazon Keyspaces related properties
     */
    @ConfigItem
    AmazonKeyspacesProperties amazonKeyspaces;

    public SchemaMigrationSettings toSchemaMigrationSettings() {
        SchemaMigrationSettings.SchemaMigrationSettingsBuilder builder = SchemaMigrationSettings.builder();
        enabled.ifPresent(builder::enabled);
        schemaHistoryTableName.ifPresent(builder::withSchemaHistoryTableName);
        builder.withVersionSettings(version.toVersionSettings());
        builder.withTemplateSettings(template.toTemplateSettings());
        builder.withLockSettings(lock.toLockSettings());
        builder.withSchemaAgreement(schemaAgreement.toSchemaAgreementSettings());
        builder.withAmazonKeyspacesSettings(amazonKeyspaces.toAmazonKeyspacesSettings());
        return builder.build();
    }
}
