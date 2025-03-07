package org.qubership.cloud.dbaas.client.cassandra.migration.exception;

public class SchemaMigrationVersionProcessingException extends Exception {
    public SchemaMigrationVersionProcessingException(Throwable cause) {
        super(cause);
    }
}