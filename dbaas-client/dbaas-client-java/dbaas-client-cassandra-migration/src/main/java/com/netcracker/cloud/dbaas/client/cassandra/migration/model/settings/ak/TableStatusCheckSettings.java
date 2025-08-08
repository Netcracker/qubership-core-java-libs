package com.netcracker.cloud.dbaas.client.cassandra.migration.model.settings.ak;

import java.time.Duration;

/**
 * Schema migration Amazon Keyspaces asynchronous DDL table status check related settings.
 * Use {@link TableStatusCheckSettingsBuilder} for creation.
 *
 * @param preDelay   preliminary delay before checking table status in system_schema_mcs.tables.
 *                   Is required because Amazon Keyspaces updates the status in
 *                   system_schema_mcs.tables asynchronously.
 * @param retryDelay retry delay for checking expected table statuses in system_schema_mcs.tables
 */
public record TableStatusCheckSettings(
        Long preDelay,
        Long retryDelay
) {
    private static final Duration DEFAULT_PRE_DELAY = Duration.ofSeconds(1);
    private static final Duration DEFAULT_RETRY_DELAY = Duration.ofMillis(500);

    public static TableStatusCheckSettingsBuilder builder() {
        return new TableStatusCheckSettingsBuilder();
    }

    public static class TableStatusCheckSettingsBuilder {
        private Long preDelay = DEFAULT_PRE_DELAY.toMillis();
        private Long retryDelay = DEFAULT_RETRY_DELAY.toMillis();

        public TableStatusCheckSettingsBuilder withPreDelay(Long preDelay) {
            if (preDelay != null)
                this.preDelay = preDelay;
            return this;
        }

        public TableStatusCheckSettingsBuilder withRetryDelay(Long retryDelay) {
            if (retryDelay != null)
                this.retryDelay = retryDelay;
            return this;
        }

        public TableStatusCheckSettings build() {
            return new TableStatusCheckSettings(preDelay, retryDelay);
        }
    }
}
