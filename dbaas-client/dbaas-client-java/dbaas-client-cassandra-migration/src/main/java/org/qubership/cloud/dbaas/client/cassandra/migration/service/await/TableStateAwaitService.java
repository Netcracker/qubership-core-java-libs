package org.qubership.cloud.dbaas.client.cassandra.migration.service.await;

import org.qubership.cloud.dbaas.client.cassandra.migration.model.operation.TableOperation;
import org.qubership.cloud.dbaas.client.cassandra.migration.model.settings.SchemaMigrationSettings;
import org.qubership.cloud.dbaas.client.cassandra.migration.service.await.ak.AmazonKeyspacesTableStateAwaitService;
import org.qubership.cloud.dbaas.client.cassandra.migration.session.SchemaMigrationSession;

import java.util.List;

public interface TableStateAwaitService {
    default void await(List<TableOperation> tableOperations) {
    }

    static TableStateAwaitService create(
            SchemaMigrationSession session,
            SchemaMigrationSettings schemaMigrationSettings
    ) {
        if (schemaMigrationSettings.amazonKeyspaces().enabled()) {
            return new AmazonKeyspacesTableStateAwaitService(session, schemaMigrationSettings.amazonKeyspaces().tableStatusCheck());
        } else {
            return new TableStateAwaitService() {
            };
        }
    }
}
