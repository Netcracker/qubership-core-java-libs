package com.netcracker.cloud.dbaas.client.cassandra.migration;

import com.datastax.oss.driver.api.core.CqlSession;
import com.netcracker.cloud.dbaas.client.cassandra.migration.model.settings.SchemaMigrationSettings;
import com.netcracker.cloud.dbaas.client.cassandra.migration.service.SchemaMigrationPreparationService;
import com.netcracker.cloud.dbaas.client.cassandra.migration.service.SchemaVersionResourceReader;
import com.netcracker.cloud.dbaas.client.cassandra.migration.service.SchemaVersionResourceReaderImpl;
import com.netcracker.cloud.dbaas.client.cassandra.migration.service.extension.AlreadyMigratedVersionsExtensionPoint;
import com.netcracker.cloud.dbaas.client.cassandra.migration.service.resource.SchemaVersionResourceFinderRegistry;
import com.netcracker.cloud.dbaas.client.cassandra.migration.session.SchemaMigrationSession;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static com.netcracker.cloud.dbaas.client.cassandra.migration.SchemaMigrationCommonConstants.MIGRATION_LOG_PREFIX;

@AllArgsConstructor
@Slf4j
public class MigrationExecutorImpl implements MigrationExecutor {

    private final SchemaMigrationSettings schemaMigrationSettings;
    private final SchemaVersionResourceReader schemaVersionResourceReader;
    private final AlreadyMigratedVersionsExtensionPoint alreadyMigratedVersionsExtensionPoint;

    @Override
    public void migrate(CqlSession session) {
        if (schemaMigrationSettings.enabled()) {
            log.info(MIGRATION_LOG_PREFIX + "Executing schema migration. AlreadyMigratedVersionsExtensionPoint " +
                    (alreadyMigratedVersionsExtensionPoint == null ? "not" : "") + "provided");

            SchemaMigrationSession schemaMigrationSession = new SchemaMigrationSession(session, schemaMigrationSettings.schemaAgreement());

            SchemaMigrationPreparationService preparationService = new SchemaMigrationPreparationService(
                    schemaMigrationSession, schemaMigrationSettings
            );
            preparationService.prepareMigration();
            log.info(MIGRATION_LOG_PREFIX + "Schema migration preparation executed.");

            try (
                    SchemaMigrationProcessor schemaMigrationProcessor = new SchemaMigrationProcessor(
                            schemaMigrationSession, schemaMigrationSettings,
                            schemaVersionResourceReader, alreadyMigratedVersionsExtensionPoint
                    )
            ) {
                schemaMigrationProcessor.migrateIfNeeded();
            }
        }
    }

    public static MigrationExecutorBuilder builder() {
        return new MigrationExecutorBuilder();
    }

    public static class MigrationExecutorBuilder {
        private SchemaMigrationSettings schemaMigrationSettings = SchemaMigrationSettings.builder().build();
        private SchemaVersionResourceReader schemaVersionResourceReader;
        private AlreadyMigratedVersionsExtensionPoint alreadyMigratedVersionsExtensionPoint;

        public MigrationExecutorBuilder withSchemaMigrationSettingsBuilder(SchemaMigrationSettings schemaMigrationSettings) {
            this.schemaMigrationSettings = schemaMigrationSettings;
            return this;
        }

        public MigrationExecutorBuilder withSchemaVersionResourceReader(SchemaVersionResourceReader schemaVersionResourceReader) {
            this.schemaVersionResourceReader = schemaVersionResourceReader;
            return this;
        }

        public MigrationExecutorBuilder withAlreadyMigratedVersionsExtensionPoint(AlreadyMigratedVersionsExtensionPoint alreadyMigratedVersionsExtensionPoint) {
            this.alreadyMigratedVersionsExtensionPoint = alreadyMigratedVersionsExtensionPoint;
            return this;
        }

        public MigrationExecutor build() {
            SchemaVersionResourceReader schemaVersionResourceReader = this.schemaVersionResourceReader == null ?
                    new SchemaVersionResourceReaderImpl(schemaMigrationSettings.version(), new SchemaVersionResourceFinderRegistry())
                    : this.schemaVersionResourceReader;
            return new MigrationExecutorImpl(schemaMigrationSettings, schemaVersionResourceReader, alreadyMigratedVersionsExtensionPoint);
        }
    }
}
