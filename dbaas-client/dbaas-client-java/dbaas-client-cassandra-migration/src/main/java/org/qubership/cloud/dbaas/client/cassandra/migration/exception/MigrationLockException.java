package org.qubership.cloud.dbaas.client.cassandra.migration.exception;

public class MigrationLockException extends Exception {
    public MigrationLockException(String message) {
        super(message);
    }
}