package org.qubership.cloud.dbaas.client.cassandra.migration.model;

public record SchemaVersionContentChecksum(
        String versionContent,
        long checksum
) {
}
