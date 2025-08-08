package com.netcracker.cloud.dbaas.client.cassandra.migration.model.settings.ak;

/**
 * Schema migration Amazon Keyspaces related settings.
 * Use {@link AmazonKeyspacesSettingsBuilder} for creation.
 *
 * @param enabled          true if Amazon Keyspaces is used instead of Cassandra
 * @param tableStatusCheck settings for asynchronous DDL table status checking
 */
public record AmazonKeyspacesSettings(
        boolean enabled,
        TableStatusCheckSettings tableStatusCheck
) {
    public static AmazonKeyspacesSettingsBuilder builder() {
        return new AmazonKeyspacesSettingsBuilder();
    }

    public static class AmazonKeyspacesSettingsBuilder {
        private boolean enabled = Boolean.FALSE;
        private TableStatusCheckSettings tableStatusCheck = TableStatusCheckSettings.builder().build();

        public AmazonKeyspacesSettingsBuilder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public AmazonKeyspacesSettingsBuilder withTableStatusCheck(TableStatusCheckSettings tableStatusCheck) {
            if (tableStatusCheck != null)
                this.tableStatusCheck = tableStatusCheck;
            return this;
        }

        public AmazonKeyspacesSettings build() {
            return new AmazonKeyspacesSettings(enabled, tableStatusCheck);
        }
    }

}
