package com.netcracker.cloud.quarkus.dbaas.cassandraclient.config.properties.migration;

import com.netcracker.cloud.dbaas.client.cassandra.migration.model.settings.ak.AmazonKeyspacesSettings;

import java.util.Optional;

public interface AmazonKeyspacesProperties {

    /**
     * Whether Amazon Keyspaces is used instead of Cassandra
     */
    Optional<Boolean> enabled();

    /**
     * Properties for asynchronous DDL table status checking
     */
    TableStatusCheckProperties tableStatusCheck();

    default AmazonKeyspacesSettings toAmazonKeyspacesSettings() {
        AmazonKeyspacesSettings.AmazonKeyspacesSettingsBuilder builder = AmazonKeyspacesSettings.builder();
        enabled().ifPresent(builder::enabled);
        builder.withTableStatusCheck(tableStatusCheck().toTableStatusCheckSettings());
        return builder.build();
    }
}
