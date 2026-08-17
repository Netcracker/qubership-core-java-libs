package com.netcracker.cloud.dbaas.client.config;

import com.netcracker.cloud.dbaas.client.cassandra.migration.MigrationExecutor;
import com.netcracker.cloud.dbaas.client.cassandra.migration.MigrationExecutorImpl;
import com.netcracker.cloud.dbaas.client.cassandra.migration.model.settings.*;
import com.netcracker.cloud.dbaas.client.cassandra.migration.model.settings.ak.AmazonKeyspacesSettings;
import com.netcracker.cloud.dbaas.client.cassandra.migration.model.settings.ak.TableStatusCheckSettings;
import com.netcracker.cloud.dbaas.client.cassandra.migration.service.SchemaVersionResourceReader;
import com.netcracker.cloud.dbaas.client.cassandra.migration.service.SchemaVersionResourceReaderImpl;
import com.netcracker.cloud.dbaas.client.cassandra.migration.service.extension.AlreadyMigratedVersionsExtensionPoint;
import com.netcracker.cloud.dbaas.client.cassandra.migration.service.resource.SchemaVersionResourceFinderRegistry;
import com.netcracker.cloud.dbaas.client.config.properties.DbaasCassandraMigrationProperties;
import com.netcracker.cloud.dbaas.client.service.migration.SpringBootJarSchemaVersionResourceFinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.netcracker.cloud.dbaas.client.cassandra.migration.service.resource.SchemaVersionResourceFinderRegistry.JAR_SCHEME;

@Configuration
@ConditionalOnProperty(value = "dbaas.cassandra.migration.enabled", havingValue = "true", matchIfMissing = true)
public class DbaasCassandraMigrationConfiguration {

    @Bean
    @ConfigurationProperties("dbaas.cassandra.migration")
    public DbaasCassandraMigrationProperties dbaasCassandraMigrationProperties() {
        return new DbaasCassandraMigrationProperties();
    }

    @Bean
    public SchemaMigrationSettings schemaMigrationSettings(
            DbaasCassandraMigrationProperties properties
    ) {
        SchemaMigrationSettings.SchemaMigrationSettingsBuilder builder = SchemaMigrationSettings.builder()
                .withSchemaHistoryTableName(properties.getSchemaHistoryTableName());

        if (properties.getVersion() != null) {
            VersionSettings versionSettings = VersionSettings.builder()
                    .withSettingsResourcePath(properties.getVersion().getSettingsResourcePath())
                    .withDirectoryPath(properties.getVersion().getDirectoryPath())
                    .withResourceNamePattern(properties.getVersion().getResourceNamePattern())
                    .build();
            builder = builder.withVersionSettings(versionSettings);
        }

        if (properties.getTemplate() != null) {
            TemplateSettings templateSettings = TemplateSettings.builder()
                    .withDefinitionsResourcePath(properties.getTemplate().getDefinitionsResourcePath())
                    .build();
            builder = builder.withTemplateSettings(templateSettings);
        }

        if (properties.getLock() != null) {
            LockSettings lockSettings = LockSettings.builder()
                    .withTableName(properties.getLock().getTableName())
                    .withLockLifetime(properties.getLock().getLockLifetime())
                    .withExtensionFailDelayRetry(properties.getLock().getExtensionFailRetryDelay())
                    .withExtensionPeriod(properties.getLock().getExtensionPeriod())
                    .withRetryDelay(properties.getLock().getRetryDelay())
                    .build();
            builder = builder.withLockSettings(lockSettings);
        }

        if (properties.getSchemaAgreement() != null) {
            SchemaAgreementSettings schemaAgreementSettings =
                    SchemaAgreementSettings.builder()
                            .withAwaitRetryDelay(properties.getSchemaAgreement().getAwaitRetryDelay())
                            .build();
            builder = builder.withSchemaAgreement(schemaAgreementSettings);
        }

        if (properties.getAmazonKeyspaces() != null) {
            AmazonKeyspacesSettings.AmazonKeyspacesSettingsBuilder akBuilder = AmazonKeyspacesSettings.builder()
                    .enabled(properties.getAmazonKeyspaces().isEnabled());
            if (properties.getAmazonKeyspaces().getTableStatusCheck() != null) {
                akBuilder.withTableStatusCheck(
                        TableStatusCheckSettings.builder()
                                .withPreDelay(properties.getAmazonKeyspaces().getTableStatusCheck().getPreDelay())
                                .withRetryDelay(properties.getAmazonKeyspaces().getTableStatusCheck().getRetryDelay())
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
