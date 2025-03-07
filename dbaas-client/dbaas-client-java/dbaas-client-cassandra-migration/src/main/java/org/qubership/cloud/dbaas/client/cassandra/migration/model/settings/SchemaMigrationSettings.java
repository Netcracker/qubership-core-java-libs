package org.qubership.cloud.dbaas.client.cassandra.migration.model.settings;

import org.qubership.cloud.dbaas.client.cassandra.migration.model.settings.ak.AmazonKeyspacesSettings;

import static org.qubership.cloud.dbaas.client.cassandra.migration.SchemaMigrationCommonConstants.DEFAULT_SCHEMA_HISTORY_TABLE_NAME;

/**
 * Schema migration settings. Use {@link SchemaMigrationSettingsBuilder} for creation.
 *
 * @param schemaHistoryTableName name of the table to store schema version history
 * @param version                version settings
 * @param template               templating settings
 * @param lock                   migration lock settings
 * @param schemaAgreement        schema agreement settings
 * @param amazonKeyspaces        Amazon Keyspaces related settings
 */
public record SchemaMigrationSettings(
        boolean enabled,
        String schemaHistoryTableName,
        VersionSettings version,
        TemplateSettings template,
        LockSettings lock,
        SchemaAgreementSettings schemaAgreement,
        AmazonKeyspacesSettings amazonKeyspaces
) {
    public static SchemaMigrationSettingsBuilder builder() {
        return new SchemaMigrationSettingsBuilder();
    }

    public static class SchemaMigrationSettingsBuilder {
        private boolean enabled = true;
        private String schemaHistoryTableName = DEFAULT_SCHEMA_HISTORY_TABLE_NAME;
        private VersionSettings version = VersionSettings.builder().build();
        private TemplateSettings template = TemplateSettings.builder().build();
        private LockSettings lock = LockSettings.builder().build();
        private SchemaAgreementSettings schemaAgreement = SchemaAgreementSettings.builder().build();
        private AmazonKeyspacesSettings amazonKeyspaces = AmazonKeyspacesSettings.builder().build();

        public SchemaMigrationSettingsBuilder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public SchemaMigrationSettingsBuilder withSchemaHistoryTableName(String schemaHistoryTableName) {
            if (schemaHistoryTableName != null)
                this.schemaHistoryTableName = schemaHistoryTableName;
            return this;
        }

        public SchemaMigrationSettingsBuilder withVersionSettings(VersionSettings version) {
            if (version != null)
                this.version = version;
            return this;
        }

        public SchemaMigrationSettingsBuilder withTemplateSettings(
                TemplateSettings template
        ) {
            if (template != null)
                this.template = template;
            return this;
        }

        public SchemaMigrationSettingsBuilder withLockSettings(LockSettings lock) {
            if (lock != null)
                this.lock = lock;
            return this;
        }

        public SchemaMigrationSettingsBuilder withSchemaAgreement(
                SchemaAgreementSettings schemaAgreement
        ) {
            if (schemaAgreement != null)
                this.schemaAgreement = schemaAgreement;
            return this;
        }

        public SchemaMigrationSettingsBuilder withAmazonKeyspacesSettings(
                AmazonKeyspacesSettings akSettings
        ) {
            if (akSettings != null)
                this.amazonKeyspaces = akSettings;
            return this;
        }

        public SchemaMigrationSettings build() {
            return new SchemaMigrationSettings(
                    enabled, schemaHistoryTableName, version, template, lock, schemaAgreement, amazonKeyspaces
            );
        }
    }
}
