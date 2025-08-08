package org.qubership.cloud.dbaas.client.cassandra.migration.model;

/**
 * Description of the previous state of schema version
 *
 * @param checksum     checksum. Will pass checksum validation if found in DB.
 * @param invalid      is the described state invalid or not. Scripts in invalid states will be reapplied even if
 *                     they have success migration status in DB.
 * @param changeReason state change reason.
 */
public record SchemaVersionPreviousState(
        String checksum,
        boolean invalid,
        String changeReason
) {
}
