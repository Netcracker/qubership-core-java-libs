package org.qubership.cloud.dbaas.client.config;

import org.qubership.cloud.dbaas.client.cassandra.migration.MigrationExecutorImpl;
import org.qubership.cloud.dbaas.client.cassandra.migration.MigrationExecutor;
import org.qubership.cloud.dbaas.client.cassandra.migration.model.settings.*;
import org.qubership.cloud.dbaas.client.cassandra.migration.model.settings.ak.AmazonKeyspacesSettings;
import org.qubership.cloud.dbaas.client.cassandra.migration.model.settings.ak.TableStatusCheckSettings;
import org.qubership.cloud.dbaas.client.cassandra.migration.service.SchemaVersionResourceReader;
import org.qubership.cloud.dbaas.client.cassandra.migration.service.SchemaVersionResourceReaderImpl;
import org.qubership.cloud.dbaas.client.cassandra.migration.service.extension.AlreadyMigratedVersionsExtensionPoint;
import org.qubership.cloud.dbaas.client.cassandra.migration.service.resource.SchemaVersionResourceFinderRegistry;
import org.qubership.cloud.dbaas.client.config.properties.DbaasCassandraMigrationProperties;
import org.qubership.cloud.dbaas.client.service.migration.SpringBootJarSchemaVersionResourceFinder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.qubership.cloud.dbaas.client.cassandra.migration.service.resource.SchemaVersionResourceFinderRegistry.JAR_SCHEME;

@Configuration
@ConditionalOnProperty(value = "dbaas.cassandra.migration.enabled", havingValue = "true", matchIfMissing = true)
@ConfigurationPropertiesScan("org.qubership.cloud.dbaas.client.config.properties")
public class DbaasCassandraMigrationConfiguration {

    @Bean
    public SchemaMigrationSettings schemaMigrationSettings(
            DbaasCassandraMigrationProperties properties
    ) {
        SchemaMigrationSettings.SchemaMigrationSettingsBuilder builder = SchemaMigrationSettings.builder()
                .withSchemaHistoryTableName(properties.schemaHistoryTableName());

        if (properties.version() != null) {
            VersionSettings versionSettings = VersionSettings.builder()
                    .withSettingsResourcePath(properties.version().settingsResourcePath())
                    .withDirectoryPath(properties.version().directoryPath())
                    .withResourceNamePattern(properties.version().resourceNamePattern())
                    .build();
            builder = builder.withVersionSettings(versionSettings);
        }

        if (properties.template() != null) {
            TemplateSettings templateSettings = TemplateSettings.builder()
                    .withDefinitionsResourcePath(properties.template().definitionsResourcePath())
                    .build();
            builder = builder.withTemplateSettings(templateSettings);
        }

        if (properties.lock() != null) {
            LockSettings lockSettings = LockSettings.builder()
                    .withTableName(properties.lock().tableName())
                    .withLockLifetime(properties.lock().lockLifetime())
                    .withExtensionFailDelayRetry(properties.lock().extensionFailRetryDelay())
                    .withExtensionPeriod(properties.lock().extensionPeriod())
                    .withRetryDelay(properties.lock().retryDelay())
                    .build();
            builder = builder.withLockSettings(lockSettings);
        }

        if (properties.schemaAgreement() != null) {
            SchemaAgreementSettings schemaAgreementSettings =
                    SchemaAgreementSettings.builder()
                            .withAwaitRetryDelay(properties.schemaAgreement().awaitRetryDelay())
                            .build();
            builder = builder.withSchemaAgreement(schemaAgreementSettings);
        }

        if (properties.amazonKeyspaces() != null) {
            AmazonKeyspacesSettings.AmazonKeyspacesSettingsBuilder akBuilder = AmazonKeyspacesSettings.builder()
                    .enabled(properties.amazonKeyspaces().enabled());
            if (properties.amazonKeyspaces().tableStatusCheck() != null) {
                akBuilder.withTableStatusCheck(
                        TableStatusCheckSettings.builder()
                                .withPreDelay(properties.amazonKeyspaces().tableStatusCheck().preDelay())
                                .withRetryDelay(properties.amazonKeyspaces().tableStatusCheck().retryDelay())
                                .build()
                );
            }

            builder = builder.withAmazonKeyspacesSettings(akBuilder.build());
        }

        return builder.build();
    }

    @Bean
    public SchemaVersionResourceReader schemaVersionResourceReader(
            SchemaMigrationSettings settings
    ) {
        SchemaVersionResourceFinderRegistry registry = new SchemaVersionResourceFinderRegistry();
        registry.register(JAR_SCHEME, new SpringBootJarSchemaVersionResourceFinder());

        return new SchemaVersionResourceReaderImpl(settings.version(), registry);
    }

    @Bean
    @ConditionalOnMissingBean
    public MigrationExecutor migrationExecutor(SchemaMigrationSettings schemaMigrationSettings,
                                               SchemaVersionResourceReader schemaVersionResourceReader,
                                               @Autowired(required = false) AlreadyMigratedVersionsExtensionPoint alreadyMigratedVersionsExtensionPoint) {
        return new MigrationExecutorImpl(schemaMigrationSettings, schemaVersionResourceReader, alreadyMigratedVersionsExtensionPoint);
    }
}
