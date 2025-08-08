package com.netcracker.cloud.dbaas.client.cassandra.migration.service;

import org.qubership.cloud.dbaas.client.cassandra.migration.model.operation.TableOperation;
import org.qubership.cloud.dbaas.client.cassandra.migration.model.operation.TableOperationType;
import org.qubership.cloud.dbaas.client.cassandra.migration.model.settings.SchemaMigrationSettings;
import org.qubership.cloud.dbaas.client.cassandra.migration.repository.AbstractMetadataAwareCqlRepository;
import org.qubership.cloud.dbaas.client.cassandra.migration.repository.SchemaMigrationPreparationRepository;
import org.qubership.cloud.dbaas.client.cassandra.migration.service.await.TableStateAwaitService;
import org.qubership.cloud.dbaas.client.cassandra.migration.session.SchemaMigrationSession;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.qubership.cloud.dbaas.client.cassandra.migration.SchemaMigrationCommonConstants.*;

@Slf4j
public class SchemaMigrationPreparationService extends AbstractMetadataAwareCqlRepository {
    private final SchemaMigrationSettings schemaMigrationSettings;
    private final TableStateAwaitService tableStateAwaitService;
    private final SchemaMigrationPreparationRepository schemaMigrationPreparationRepository;

    public SchemaMigrationPreparationService(
            SchemaMigrationSession session,
            SchemaMigrationSettings schemaMigrationSettings
    ) {
        super(session);
        this.tableStateAwaitService = TableStateAwaitService.create(
                session, schemaMigrationSettings
        );
        this.schemaMigrationSettings = schemaMigrationSettings;
        this.schemaMigrationPreparationRepository = new SchemaMigrationPreparationRepository(
                session, schemaMigrationSettings
        );
    }

    public void prepareMigration() {
        List<TableOperation> preparationTableOperations = new ArrayList<>();
        preparationTableOperations.addAll(prepareLockTable(schemaMigrationSettings.lock().tableName()));
        preparationTableOperations.addAll(prepareSchemaHistoryTable(schemaMigrationSettings.schemaHistoryTableName()));
        tableStateAwaitService.await(preparationTableOperations);
    }

    private List<TableOperation> prepareLockTable(String tableName) {
        if (tableExists(tableName)) {
            log.info(MIGRATION_LOG_PREFIX + "Lock table {} already exists", tableName);
            return Collections.emptyList();
        }
        log.info(MIGRATION_LOG_PREFIX + "Start creating lock table {}", tableName);
        schemaMigrationPreparationRepository.createLockTable();
        log.info(MIGRATION_LOG_PREFIX + "Finished creating lock table {}", tableName);
        return Collections.singletonList(
                new TableOperation(tableName, TableOperationType.CREATE)
        );
    }

    private List<TableOperation> prepareSchemaHistoryTable(String tableName) {
        if (tableExists(tableName)) {
            log.info(MIGRATION_LOG_PREFIX + "Schema history table {} already exists", tableName);
            return Collections.emptyList();
        }
        log.info(MIGRATION_LOG_PREFIX + "Start creating schema history table {}", tableName);
        schemaMigrationPreparationRepository.createSchemaHistoryTable();
        log.info(MIGRATION_LOG_PREFIX + "Created schema history table {}", tableName);
        return Collections.singletonList(
                new TableOperation(tableName, TableOperationType.CREATE)
        );
    }
}
