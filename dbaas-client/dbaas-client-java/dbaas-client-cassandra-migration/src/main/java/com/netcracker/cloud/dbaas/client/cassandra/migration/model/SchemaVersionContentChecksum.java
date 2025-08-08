package com.netcracker.cloud.dbaas.client.cassandra.migration.model;

public record SchemaVersionContentChecksum(
        String versionContent,
        long checksum
) {
}
