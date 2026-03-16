package com.netcracker.cloud.dbaas.client.cassandra.migration.model;

public record SchemaVersionMigrationResult(
        boolean success,
        Exception exception,
        long executionTime
) {
}
