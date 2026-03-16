package com.netcracker.cloud.quarkus.dbaas.cassandraclient.config.properties.migration;

import com.netcracker.cloud.dbaas.client.cassandra.migration.model.settings.ak.TableStatusCheckSettings;

import java.util.Optional;

public interface TableStatusCheckProperties {

    /**
     * Preliminary delay before checking table status in system_schema_mcs.tables.
     * Is required because Amazon Keyspaces updates the status in
     * system_schema_mcs.tables asynchronously.
     */
    Optional<Long> preDelay();

    /**
     * Retry delay for checking expected table statuses in system_schema_mcs.tables
     */
    Optional<Long> retryDelay();

    default TableStatusCheckSettings toTableStatusCheckSettings() {
        TableStatusCheckSettings.TableStatusCheckSettingsBuilder builder = TableStatusCheckSettings.builder();
        preDelay().ifPresent(builder::withPreDelay);
        retryDelay().ifPresent(builder::withRetryDelay);
        return builder.build();
    }
}
