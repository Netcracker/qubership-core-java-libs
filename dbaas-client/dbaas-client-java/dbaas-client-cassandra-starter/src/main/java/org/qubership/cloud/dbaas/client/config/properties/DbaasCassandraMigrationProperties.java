package org.qubership.cloud.dbaas.client.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("dbaas.cassandra.migration")
public record DbaasCassandraMigrationProperties(
        boolean enabled,
        String schemaHistoryTableName,
        VersionProperties version,
        TemplateProperties template,
        LockProperties lock,
        SchemaAgreementProperties schemaAgreement,
        AmazonKeyspacesProperties amazonKeyspaces
) {
    public record VersionProperties(
            String settingsResourcePath,
            String directoryPath,
            String resourceNamePattern
    ) {
    }

    public record TemplateProperties(
            String definitionsResourcePath
    ) {
    }

    public record LockProperties(
            String tableName,
            Long retryDelay,
            Long lockLifetime,
            Long extensionPeriod,
            Long extensionFailRetryDelay
    ) {
    }

    public record SchemaAgreementProperties(
            Long awaitRetryDelay
    ) {
    }

    public record AmazonKeyspacesProperties(
            boolean enabled,
            TableStatusCheck tableStatusCheck
    ) {
        public record TableStatusCheck(
                Long preDelay,
                Long retryDelay
        ) {
        }
    }
}
