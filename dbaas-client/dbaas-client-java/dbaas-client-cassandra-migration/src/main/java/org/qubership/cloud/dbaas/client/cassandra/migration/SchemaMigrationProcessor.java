package org.qubership.cloud.dbaas.client.cassandra.migration;

import org.qubership.cloud.dbaas.client.cassandra.migration.exception.SchemaMigrationException;
import org.qubership.cloud.dbaas.client.cassandra.migration.exception.SchemaMigrationVersionProcessingException;
import org.qubership.cloud.dbaas.client.cassandra.migration.model.SchemaVersionFromDb;
import org.qubership.cloud.dbaas.client.cassandra.migration.model.SchemaVersionFromResource;
import org.qubership.cloud.dbaas.client.cassandra.migration.model.SchemaVersionPreviousState;
import org.qubership.cloud.dbaas.client.cassandra.migration.model.SchemaVersionToApply;
import org.qubership.cloud.dbaas.client.cassandra.migration.model.settings.SchemaMigrationSettings;
import org.qubership.cloud.dbaas.client.cassandra.migration.repository.SchemaHistoryRepository;
import org.qubership.cloud.dbaas.client.cassandra.migration.service.SchemaHistoryService;
import org.qubership.cloud.dbaas.client.cassandra.migration.service.SchemaVersionResourceReader;
import org.qubership.cloud.dbaas.client.cassandra.migration.service.compatibility.AlreadyMigratedVersionsService;
import org.qubership.cloud.dbaas.client.cassandra.migration.service.extension.AlreadyMigratedVersionsExtensionPoint;
import org.qubership.cloud.dbaas.client.cassandra.migration.service.lock.MigrationLockService;
import org.qubership.cloud.dbaas.client.cassandra.migration.session.SchemaMigrationSession;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.versioning.ComparableVersion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.qubership.cloud.dbaas.client.cassandra.migration.SchemaMigrationCommonConstants.*;


@Slf4j
@AllArgsConstructor
public class SchemaMigrationProcessor implements AutoCloseable {
    public enum DeltaCalculationStage {
        PRIMARY, PRELIMINARY
    }

    private final MigrationLockService migrationLockService;
    private final SchemaHistoryService schemaHistoryService;
    private final SchemaVersionResourceReader schemaVersionResourceReader;
    private final SchemaMigrationVersionProcessor schemaMigrationVersionProcessor;
    private final AlreadyMigratedVersionsService alreadyMigratedVersionsService;

    public SchemaMigrationProcessor(
            SchemaMigrationSession session,
            SchemaMigrationSettings schemaMigrationSettings,
            SchemaVersionResourceReader schemaVersionResourceReader,
            AlreadyMigratedVersionsExtensionPoint extensionPoint
    ) {
        this.schemaVersionResourceReader = schemaVersionResourceReader;
        SchemaHistoryRepository schemaHistoryRepository = new SchemaHistoryRepository(session, schemaMigrationSettings);
        this.schemaHistoryService = new SchemaHistoryService(schemaHistoryRepository);
        this.migrationLockService = new MigrationLockService(session, schemaMigrationSettings);
        this.schemaMigrationVersionProcessor = new SchemaMigrationVersionProcessor(
                session, schemaHistoryService, migrationLockService, schemaMigrationSettings
        );
        this.alreadyMigratedVersionsService = new AlreadyMigratedVersionsService(
                session, schemaHistoryRepository, extensionPoint
        );
    }

    List<SchemaVersionToApply> calculateSchemaVersionsToApply(
            List<SchemaVersionFromResource> versionsFromResources, DeltaCalculationStage calculationStage
    ) {
        log.info(
                MIGRATION_LOG_PREFIX + "Start calculating {} schema migration changeset.",
                calculationStage
        );

        List<SchemaVersionFromDb> versionsFromDb = schemaHistoryService.getSchemaVersionsFromDb();
        log.info(MIGRATION_LOG_PREFIX + "Versions found in DB {}", versionsFromDb);
        if (calculationStage == DeltaCalculationStage.PRIMARY && CollectionUtils.isEmpty(versionsFromDb)) {
            versionsFromDb = alreadyMigratedVersionsService.insertAlreadyMigratedVersions(versionsFromResources);
            log.info(MIGRATION_LOG_PREFIX + "Versions found in DB updated by extension point {}", versionsFromDb);
        }

        Map<ComparableVersion, SchemaVersionFromDb> dbVersionsMap = versionsFromDb.stream()
                .collect(Collectors.toMap(SchemaVersionFromDb::version, Function.identity()));
        int globalInstalledRank = versionsFromDb.stream().map(SchemaVersionFromDb::installedRank)
                .max(Integer::compareTo).orElse(0);

        List<SchemaVersionToApply> result = new ArrayList<>();
        for (SchemaVersionFromResource versionFromResource : versionsFromResources) {
            SchemaVersionToApply schemaVersionToApply = createSchemaVersionToApply(
                    versionFromResource, dbVersionsMap.get(versionFromResource.version()), globalInstalledRank
            );
            if (schemaVersionToApply != null) {
                if (schemaVersionToApply.installedRank() > globalInstalledRank) {
                    globalInstalledRank = schemaVersionToApply.installedRank();
                }
                result.add(schemaVersionToApply);
            }
        }
        log.info(MIGRATION_LOG_PREFIX + "End calculating {} schema migration changeset", calculationStage);
        return result;
    }

    private SchemaVersionToApply createSchemaVersionToApply(
            SchemaVersionFromResource versionFromResource, SchemaVersionFromDb versionFromDb,
            int globalInstalledRank
    ) {
        if (versionFromDb == null) {
            int installedRank = ++globalInstalledRank;
            log.info(
                    MIGRATION_LOG_PREFIX + "Schema version {} was not installed. Will be installed with rank {}",
                    versionFromResource.version(), installedRank
            );
            return new SchemaVersionToApply(
                    installedRank, versionFromResource.version(), versionFromResource.description(), versionFromResource.type(),
                    versionFromResource.resourcePath(), versionFromResource.script(), versionFromResource.checksum(),
                    versionFromResource.settings()
            );
        }

        String description = versionFromDb.description();
        SchemaVersionPreviousState previousState = null;
        boolean checksumMatch = versionFromDb.checksum() == versionFromResource.checksum();
        String checksumLog = "";
        if (!checksumMatch) {
            if (versionFromResource.settings().previousStates() != null) {
                previousState = versionFromResource.settings().previousStates().stream()
                        .filter(state -> StringUtils.equals(String.valueOf(versionFromDb.checksum()), state.checksum()))
                        .findFirst().orElse(null);
            }
            if (previousState == null) {
                throw new SchemaMigrationException(String.format(
                        "DB checksum %s does not match with resource checksum %s and not included in previousStates for %s",
                        versionFromDb.checksum(), versionFromResource.checksum(), versionFromResource.resourcePath()
                ));
            }
            checksumLog = " (current checksum " + versionFromResource.checksum() + ", previous state checksum " + versionFromDb.checksum() +
                    " invalid=" + previousState.invalid() + ")";
            description = addChecksumUpdateDescription(description, versionFromDb.checksum(), versionFromResource.checksum());
        }

        if (versionFromDb.success() && (checksumMatch || !previousState.invalid())) {
            log.info(
                    MIGRATION_LOG_PREFIX + "Schema version {} already installed with rank {}{}, skipping", versionFromDb.version(),
                    versionFromDb.installedRank(), checksumLog
            );
            return null;
        }

        log.info(
                MIGRATION_LOG_PREFIX + "Schema version {} has success={} installation{}. Will be reinstalled with rank {}",
                versionFromDb.version(), versionFromDb.success(), checksumLog, versionFromDb.installedRank()
        );
        return new SchemaVersionToApply(
                versionFromDb.installedRank(), versionFromResource.version(), description, versionFromResource.type(),
                versionFromResource.resourcePath(), versionFromResource.script(), versionFromResource.checksum(),
                versionFromResource.settings()
        );
    }

    private static String addChecksumUpdateDescription(
            String description, long oldChecksum, long newChecksum
    ) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isNotEmpty(description)) {
            sb.append(description).append("\n");
        }
        sb.append("Checksum updated: ").append(oldChecksum).append("->").append(newChecksum);
        return sb.toString();
    }

    public void migrateIfNeeded() {
        try {
            List<SchemaVersionFromResource> versionsFromResources = schemaVersionResourceReader.readSchemaVersionResources();
            List<SchemaVersionToApply> versionsToApply = calculateSchemaVersionsToApply(versionsFromResources, DeltaCalculationStage.PRELIMINARY);
            if (CollectionUtils.isEmpty(versionsToApply)) {
                log.info(MIGRATION_LOG_PREFIX + "Database schema migration not required.");
                return;
            }
            migrate(versionsFromResources);
        } catch (Throwable e) {
            String msg = "Database schema migration failed.";
            log.error(MIGRATION_LOG_PREFIX + msg, e);
            if (e instanceof Error err) {
                throw err;
            } else if (e instanceof SchemaMigrationException sme) {
                throw sme;
            } else {
                throw new SchemaMigrationException(msg, e);
            }
        }
    }

    private void migrate(
            List<SchemaVersionFromResource> versionsFromResources
    ) throws SchemaMigrationVersionProcessingException {
        log.info(MIGRATION_LOG_PREFIX + "Start database schema migration");
        long t1 = System.currentTimeMillis();

        try {
            migrationLockService.lockOrWaitFor();
            List<SchemaVersionToApply> versionsToApply = calculateSchemaVersionsToApply(versionsFromResources, DeltaCalculationStage.PRIMARY);
            if (CollectionUtils.isEmpty(versionsToApply)) {
                log.info(MIGRATION_LOG_PREFIX + "Database schema migration not required.");
                return;
            }

            for (SchemaVersionToApply versionToApply : versionsToApply) {
                schemaMigrationVersionProcessor.process(versionToApply);
            }
        } finally {
            migrationLockService.unlock();
            log.info(MIGRATION_LOG_PREFIX + "Stop database schema migration. Total time {} ms", System.currentTimeMillis() - t1);
        }
    }

    @Override
    public void close() {
        migrationLockService.close();
    }
}
