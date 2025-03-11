package org.qubership.cloud.quarkus.dbaas.cassandraclient.config.properties.migration;

import org.qubership.cloud.dbaas.client.cassandra.migration.model.settings.VersionSettings;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Optional;

@ConfigGroup
@Getter
@Accessors(fluent = true)
public class VersionProperties {

    /**
     * Resource path to get additional schema version settings
     */
    @ConfigItem
    Optional<String> settingsResourcePath;

    /**
     * Directory path to scan for schema version resources
     */
    @ConfigItem
    Optional<String> directoryPath;

    /**
     * Pattern to get information about schema version from resource name
     * Must contain the following matching groups in specified order:
     * <ol>
     *   <li>version</li>
     *   <li>description</li>
     *   <li>resource type</li>
     * </ol>
     */
    @ConfigItem
    Optional<String> resourceNamePattern;

    public VersionSettings toVersionSettings() {
        VersionSettings.VersionSettingsBuilder builder = VersionSettings.builder();
        settingsResourcePath.ifPresent(builder::withSettingsResourcePath);
        directoryPath.ifPresent(builder::withDirectoryPath);
        resourceNamePattern.ifPresent(builder::withResourceNamePattern);
        return builder.build();
    }
}
