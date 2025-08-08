package org.qubership.cloud.dbaas.client.cassandra.migration.model;

import org.apache.maven.artifact.versioning.ComparableVersion;

public record SchemaVersionToApply(
        int installedRank,
        ComparableVersion version,
        String description,
        String type,
        String resourcePath,
        String script,
        long checksum,
        SchemaVersionSettings settings
) {
}
