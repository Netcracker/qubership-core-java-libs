package org.qubership.cloud.quarkus.dbaas.cassandraclient.config.properties.migration;

import org.qubership.cloud.dbaas.client.cassandra.migration.model.settings.ak.AmazonKeyspacesSettings;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Optional;

@ConfigGroup
@Getter
@Accessors(fluent = true)
public class AmazonKeyspacesProperties {

    /**
     * Whether Amazon Keyspaces is used instead of Cassandra
     */
    @ConfigItem
    Optional<Boolean> enabled;

    /**
     * Properties for asynchronous DDL table status checking
     */
    @ConfigItem
    TableStatusCheckProperties tableStatusCheck;

    public AmazonKeyspacesSettings toAmazonKeyspacesSettings() {
        AmazonKeyspacesSettings.AmazonKeyspacesSettingsBuilder builder = AmazonKeyspacesSettings.builder();
        enabled.ifPresent(builder::enabled);
        builder.withTableStatusCheck(tableStatusCheck.toTableStatusCheckSettings());
        return builder.build();
    }
}
