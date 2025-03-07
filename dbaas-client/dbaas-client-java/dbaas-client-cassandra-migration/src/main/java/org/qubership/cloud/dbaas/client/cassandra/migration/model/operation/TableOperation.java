package org.qubership.cloud.dbaas.client.cassandra.migration.model.operation;

/**
 * Operation performed with the table by schema version.
 * May be used e.g. for asynchronous DDL completion waits in case of Amazon Keyspaces usage
 *
 * @param tableName table name
 * @param operationType performed operation
 */
public record TableOperation(
        String tableName,
        TableOperationType operationType
) {
}
