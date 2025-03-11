package org.qubership.cloud.quarkus.dbaas.cassandraclient.config.properties.migration;

import org.qubership.cloud.dbaas.client.cassandra.migration.model.settings.LockSettings;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Optional;

@ConfigGroup
@Getter
@Accessors(fluent = true)
public class LockProperties {

    /**
     * Name of the table for migration locks holding
     */
    @ConfigItem
    Optional<String> tableName;

    /**
     * Delay between attempts to acquire the lock
     */
    @ConfigItem
    Optional<Long> retryDelay;

    /**
     * Lock lifetime
     */
    @ConfigItem
    Optional<Long> lockLifetime;

    /**
     * Lock extension period
     */
    @ConfigItem
    Optional<Long> extensionPeriod;

    /**
     * Lock extension delay after the extension failure. Will be applied until the extension success or lock lifetime passes
     */
    @ConfigItem
    Optional<Long> extensionFailRetryDelay;

    public LockSettings toLockSettings() {
        LockSettings.LockSettingsBuilder builder = LockSettings.builder();
        tableName.ifPresent(builder::withTableName);
        retryDelay.ifPresent(builder::withRetryDelay);
        lockLifetime.ifPresent(builder::withLockLifetime);
        extensionPeriod.ifPresent(builder::withExtensionPeriod);
        extensionFailRetryDelay.ifPresent(builder::withExtensionFailDelayRetry);
        return builder.build();
    }
}
