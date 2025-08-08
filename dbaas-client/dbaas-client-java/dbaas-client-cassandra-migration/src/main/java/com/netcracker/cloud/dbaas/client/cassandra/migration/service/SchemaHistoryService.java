package com.netcracker.cloud.dbaas.client.cassandra.migration.service;

import com.netcracker.cloud.dbaas.client.cassandra.migration.model.SchemaVersionFromDb;
import com.netcracker.cloud.dbaas.client.cassandra.migration.model.SchemaVersionMigrationResult;
import com.netcracker.cloud.dbaas.client.cassandra.migration.model.SchemaVersionToApply;
import com.netcracker.cloud.dbaas.client.cassandra.migration.repository.SchemaHistoryRepository;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static com.netcracker.cloud.dbaas.client.cassandra.migration.SchemaMigrationCommonConstants.*;

@Slf4j
public class SchemaHistoryService {
    private final SchemaHistoryRepository schemaHistoryRepository;

    public SchemaHistoryService(SchemaHistoryRepository schemaHistoryRepository) {
        this.schemaHistoryRepository = schemaHistoryRepository;
    }

    public List<SchemaVersionFromDb> getSchemaVersionsFromDb() {
        return schemaHistoryRepository.selectAll();
    }

    public void insertPreProcessVersionData(SchemaVersionToApply versionToApply) {
        schemaHistoryRepository.insert(
                versionToApply.installedRank(), versionToApply.version().toString(), versionToApply.description(),
                versionToApply.type(), versionToApply.resourcePath(), versionToApply.checksum(),
                HOST_NAME, false, "pre-execute"
        );
        log.info(MIGRATION_LOG_PREFIX + "Inserted pre-execute data for version {}", versionToApply.version());
    }

    public void insertPostProcessVersionData(
            SchemaVersionToApply versionToApply, SchemaVersionMigrationResult migrationResult
    ) {
        String extraInfo = migrationResult.exception() == null ? null : migrationResult.exception().getMessage();
        schemaHistoryRepository.updateExecutionInfo(
                versionToApply.installedRank(), migrationResult.executionTime(), migrationResult.success(), extraInfo
        );
        log.info(
                MIGRATION_LOG_PREFIX + "Inserted post-execute data for version {}, status {}",
                versionToApply.version(), migrationResult.success()
        );
    }
}
