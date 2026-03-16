package com.netcracker.cloud.dbaas.client.cassandra.migration.model;

/**
 * Information about already migrated version
 *
 * @param version schema version string representation
 * @param success was the migration successful or not
 */
public record AlreadyMigratedVersion(
        String version,
        boolean success
) {
}
