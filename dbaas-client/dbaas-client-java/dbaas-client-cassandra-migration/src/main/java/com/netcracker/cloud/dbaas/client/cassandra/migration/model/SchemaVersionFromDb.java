package com.netcracker.cloud.dbaas.client.cassandra.migration.model;

import org.apache.maven.artifact.versioning.ComparableVersion;

public record SchemaVersionFromDb(
        ComparableVersion version,
        long checksum,
        boolean success,
        int installedRank,
        String description
) {
}
