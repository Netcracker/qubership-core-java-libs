package com.netcracker.cloud.dbaas.client.config.properties;

import lombok.Data;

@Data
public class DbaasCassandraMigrationProperties {
    private boolean enabled;
    private String schemaHistoryTableName;
    private VersionProperties version;
    private TemplateProperties template;
    private LockProperties lock;
    private SchemaAgreementProperties schemaAgreement;
    private AmazonKeyspacesProperties amazonKeyspaces;

    @Data
    public static class VersionProperties {
        private String settingsResourcePath;
        private String directoryPath;
        private String resourceNamePattern;
    }

    @Data
    public static class TemplateProperties {
        private String definitionsResourcePath;
    }

    @Data
    public static class LockProperties {
        private String tableName;
        private Long retryDelay;
        private Long lockLifetime;
        private Long extensionPeriod;
        private Long extensionFailRetryDelay;
    }

    @Data
    public static class SchemaAgreementProperties {
        private Long awaitRetryDelay;
    }

    @Data
    public static class AmazonKeyspacesProperties {
        private boolean enabled;
        private TableStatusCheck tableStatusCheck;

        @Data
        public static class TableStatusCheck {
            private Long preDelay;
            private Long retryDelay;
        }
    }
}
