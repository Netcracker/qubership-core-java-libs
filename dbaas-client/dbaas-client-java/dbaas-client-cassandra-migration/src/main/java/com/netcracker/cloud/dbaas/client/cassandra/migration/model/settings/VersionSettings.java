package org.qubership.cloud.dbaas.client.cassandra.migration.model.settings;

import org.qubership.cloud.dbaas.client.cassandra.migration.model.SchemaVersionSettings;

import static org.qubership.cloud.dbaas.client.cassandra.migration.SchemaMigrationCommonConstants.DEFAULT_VERSIONS_DIRECTORY_PATH;
import static org.qubership.cloud.dbaas.client.cassandra.migration.SchemaMigrationCommonConstants.DEFAULT_RESOURCE_NAME_PATTERN;
import static org.qubership.cloud.dbaas.client.cassandra.migration.SchemaMigrationCommonConstants.DEFAULT_SETTINGS_RESOURCE_PATH;

/**
 * Schema migration version settings.
 * Use {@link VersionSettingsBuilder} for creation.
 *
 * @param settingsResourcePath resource path to get additional schema version settings. See also {@link SchemaVersionSettings}
 * @param directoryPath        directory path to scan for schema version resources
 * @param resourceNamePattern  pattern to get information about schema version from resource name
 *                             Must contain the following matching groups in specified order:
 *                             <ol>
 *                               <li>version</li>
 *                               <li>description</li>
 *                               <li>resource type</li>
 *                             </ol>
 */
public record VersionSettings(
        String settingsResourcePath,
        String directoryPath,
        String resourceNamePattern
) {

    public static VersionSettingsBuilder builder() {
        return new VersionSettingsBuilder();
    }

    public static class VersionSettingsBuilder {
        String settingsResourcePath = DEFAULT_SETTINGS_RESOURCE_PATH;
        String directoryPath = DEFAULT_VERSIONS_DIRECTORY_PATH;
        String resourceNamePattern = DEFAULT_RESOURCE_NAME_PATTERN;

        public VersionSettingsBuilder withSettingsResourcePath(String settingsResourcePath) {
            if (settingsResourcePath != null)
                this.settingsResourcePath = settingsResourcePath;
            return this;
        }

        public VersionSettingsBuilder withDirectoryPath(String directoryPath) {
            if (directoryPath != null)
                this.directoryPath = directoryPath;
            return this;
        }

        public VersionSettingsBuilder withResourceNamePattern(String resourceNamePattern) {
            if (resourceNamePattern != null)
                this.resourceNamePattern = resourceNamePattern;
            return this;
        }

        public VersionSettings build() {
            return new VersionSettings(settingsResourcePath, directoryPath, resourceNamePattern);
        }
    }
}
