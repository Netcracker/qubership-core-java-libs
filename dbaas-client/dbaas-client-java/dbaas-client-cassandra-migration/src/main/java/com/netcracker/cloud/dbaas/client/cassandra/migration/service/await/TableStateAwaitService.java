package com.netcracker.cloud.dbaas.client.cassandra.migration.service.await;

import com.netcracker.cloud.dbaas.client.cassandra.migration.model.operation.TableOperation;
import com.netcracker.cloud.dbaas.client.cassandra.migration.model.settings.SchemaMigrationSettings;
import com.netcracker.cloud.dbaas.client.cassandra.migration.service.await.ak.AmazonKeyspacesTableStateAwaitService;
import com.netcracker.cloud.dbaas.client.cassandra.migration.session.SchemaMigrationSession;

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
