package org.qubership.cloud.dbaas.client.cassandra.migration.service.await.ak;

record ExpectedOperationResult(
        String tableName,
        ExpectedTableStatus status,
        String dbStatus
) {
}
