package com.netcracker.cloud.quarkus.dbaas.cassandraclient.config.properties.migration;

import com.netcracker.cloud.dbaas.client.cassandra.migration.model.settings.LockSettings;

import java.util.Optional;

public interface LockProperties {

    /**
     * Name of the table for migration locks holding
     */
    Optional<String> tableName();

    /**
     * Delay between attempts to acquire the lock
     */
    Optional<Long> retryDelay();

    /**
     * Lock lifetime
     */
    Optional<Long> lockLifetime();

    /**
     * Lock extension period
     */
    Optional<Long> extensionPeriod();

    /**
     * Lock extension delay after the extension failure. Will be applied until the extension success or lock lifetime passes
     */
    Optional<Long> extensionFailRetryDelay();

    default LockSettings toLockSettings() {
        LockSettings.LockSettingsBuilder builder = LockSettings.builder();
        tableName().ifPresent(builder::withTableName);
        retryDelay().ifPresent(builder::withRetryDelay);
        lockLifetime().ifPresent(builder::withLockLifetime);
        extensionPeriod().ifPresent(builder::withExtensionPeriod);
        extensionFailRetryDelay().ifPresent(builder::withExtensionFailDelayRetry);
        return builder.build();
    }
}
