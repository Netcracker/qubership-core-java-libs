package org.qubership.cloud.quarkus.dbaas.cassandraclient.config.properties.migration;

import org.qubership.cloud.dbaas.client.cassandra.migration.model.settings.ak.TableStatusCheckSettings;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Optional;

@ConfigGroup
@Getter
@Accessors(fluent = true)
public class TableStatusCheckProperties {

    /**
     * Preliminary delay before checking table status in system_schema_mcs.tables.
     * Is required because Amazon Keyspaces updates the status in
     * system_schema_mcs.tables asynchronously.
     */
    @ConfigItem
    Optional<Long> preDelay;

    /**
     * Retry delay for checking expected table statuses in system_schema_mcs.tables
     */
    @ConfigItem
    Optional<Long> retryDelay;

    public TableStatusCheckSettings toTableStatusCheckSettings() {
        TableStatusCheckSettings.TableStatusCheckSettingsBuilder builder = TableStatusCheckSettings.builder();
        preDelay.ifPresent(builder::withPreDelay);
        retryDelay.ifPresent(builder::withRetryDelay);
        return builder.build();
    }
}
