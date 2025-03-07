package org.qubership.cloud.dbaas.client.cassandra.migration.model.operation;

public enum TableOperationType {
    /**
     * Table was updated by schema version
     */
    UPDATE,

    /**
     * Table was created by schema version
     */
    CREATE,

    /**
     * Table was dropped by schema version
     */
    DROP
}
