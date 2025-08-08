package com.netcracker.cloud.dbaas.client.cassandra.migration.exception;

public class SchemaMigrationException extends RuntimeException {

    public SchemaMigrationException(String message) {
        super(message);
    }

    public SchemaMigrationException(Throwable cause) {
        super(cause);
    }

    public SchemaMigrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
