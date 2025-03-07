package org.qubership.cloud.dbaas.client.cassandra.migration.model;

import org.apache.maven.artifact.versioning.ComparableVersion;

/**
 * Schema version information read from resource
 *
 * @param version         schema version
 * @param versionAsString schema version {@link String} representation
 * @param description     schema version description
 * @param type            schema version resource type
 * @param resourcePath    schema version resource path
 * @param script          schema version script
 * @param checksum        schema version script checksum
 * @param settings        additional schema version settings
 */
public record SchemaVersionFromResource(
        ComparableVersion version,
        String versionAsString,
        String description,
        String type,
        String resourcePath,
        String script,
        long checksum,
        SchemaVersionSettings settings
) {
}
