package org.qubership.cloud.dbaas.client.cassandra.migration.model.settings;

import java.time.Duration;

import static org.qubership.cloud.dbaas.client.cassandra.migration.SchemaMigrationCommonConstants.DEFAULT_SCHEMA_MIGRATION_LOCK_TABLE_NAME;

/**
 * Schema migration lock settings. Due to the Cassandra specific lock is acquired for the limited time period and then
 * periodically extended.
 * Use {@link LockSettingsBuilder} for creation.
 *
 * @param tableName               name of the table for migration locks holding
 * @param retryDelay              delay between attempts to acquire the lock
 * @param lockLifetime            lock lifetime
 * @param extensionPeriod         lock extension period
 * @param extensionFailRetryDelay lock extension delay after the extension failure. Will be applied until the extension success or
 *                                {@link LockSettings#lockLifetime} is passed
 */
public record LockSettings(
        String tableName,
        Long retryDelay,
        Long lockLifetime,
        Long extensionPeriod,
        Long extensionFailRetryDelay
) {
    private static final Duration DEFAULT_RETRY_DELAY_MS = Duration.ofSeconds(5);
    private static final Duration DEFAULT_LOCK_LIFETIME = Duration.ofSeconds(60);
    private static final Duration DEFAULT_EXTENSION_PERIOD = Duration.ofSeconds(5);
    private static final Duration DEFAULT_EXTENSION_FAIL_RETRY_DELAY = Duration.ofMillis(500);

    public static LockSettingsBuilder builder() {
        return new LockSettingsBuilder();
    }

    public static class LockSettingsBuilder {
        private String tableName = DEFAULT_SCHEMA_MIGRATION_LOCK_TABLE_NAME;
        private Long retryDelay = DEFAULT_RETRY_DELAY_MS.toMillis();
        private Long lockLifetime = DEFAULT_LOCK_LIFETIME.toMillis();
        private Long extensionPeriod = DEFAULT_EXTENSION_PERIOD.toMillis();
        private Long extensionFailRetryDelay = DEFAULT_EXTENSION_FAIL_RETRY_DELAY.toMillis();

        public LockSettingsBuilder withTableName(String tableName) {
            if (tableName != null)
                this.tableName = tableName;
            return this;
        }

        public LockSettingsBuilder withLockLifetime(Long lockLifetime) {
            if (lockLifetime != null)
                this.lockLifetime = lockLifetime;
            return this;
        }

        public LockSettingsBuilder withExtensionPeriod(Long extensionPeriod) {
            if (extensionPeriod != null)
                this.extensionPeriod = extensionPeriod;
            return this;
        }

        public LockSettingsBuilder withExtensionFailDelayRetry(Long extensionFailRetryDelay) {
            if (extensionFailRetryDelay != null)
                this.extensionFailRetryDelay = extensionFailRetryDelay;
            return this;
        }

        public LockSettingsBuilder withRetryDelay(Long retryDelay) {
            if (retryDelay != null)
                this.retryDelay = retryDelay;
            return this;
        }

        public LockSettings build() {
            return new LockSettings(
                    tableName, retryDelay, lockLifetime, extensionPeriod, extensionFailRetryDelay
            );
        }
    }
}
