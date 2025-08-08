package org.qubership.cloud.dbaas.client.cassandra.migration.model;


import org.qubership.cloud.dbaas.client.cassandra.migration.model.operation.TableOperation;

import java.util.List;

/**
 * Additional information about schema version
 *
 * @param ignoreErrorPatterns error message patterns to be ignored during the schema version execution.
 * @param tableOperations     list from {@link TableOperation} which contains table name and describing operations
 *                            performed with the tables within the schema version.
 * @param previousStates      information about previous states of the resource
 */
public record SchemaVersionSettings(
        List<String> ignoreErrorPatterns,
        List<TableOperation> tableOperations,
        List<SchemaVersionPreviousState> previousStates
) {
}
