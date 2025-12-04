package com.netcracker.cloud.quarkus.dbaas.cassandraclient.config.properties.migration;

import com.netcracker.cloud.dbaas.client.cassandra.migration.model.settings.VersionSettings;

import java.util.Optional;

public interface VersionProperties {

    /**
     * Resource path to get additional schema version settings
     */
    Optional<String> settingsResourcePath();

    /**
     * Directory path to scan for schema version resources
     */
    Optional<String> directoryPath();

    /**
     * Pattern to get information about schema version from resource name
     * Must contain the following matching groups in specified order:
     * <ol>
     *   <li>version</li>
     *   <li>description</li>
     *   <li>resource type</li>
     * </ol>
     */
    Optional<String> resourceNamePattern();

    default VersionSettings toVersionSettings() {
        VersionSettings.VersionSettingsBuilder builder = VersionSettings.builder();
        settingsResourcePath().ifPresent(builder::withSettingsResourcePath);
        directoryPath().ifPresent(builder::withDirectoryPath);
        resourceNamePattern().ifPresent(builder::withResourceNamePattern);
        return builder.build();
    }
}
