package com.netcracker.cloud.dbaas.client.cassandra.migration;

import com.datastax.oss.driver.api.core.DriverException;
import com.netcracker.cloud.dbaas.client.cassandra.migration.exception.SchemaMigrationException;
import com.netcracker.cloud.dbaas.client.cassandra.migration.exception.SchemaMigrationVersionProcessingException;
import com.netcracker.cloud.dbaas.client.cassandra.migration.model.SchemaVersionMigrationResult;
import com.netcracker.cloud.dbaas.client.cassandra.migration.model.SchemaVersionToApply;
import com.netcracker.cloud.dbaas.client.cassandra.migration.model.settings.SchemaMigrationSettings;
import com.netcracker.cloud.dbaas.client.cassandra.migration.model.settings.ak.AmazonKeyspacesSettings;
import com.netcracker.cloud.dbaas.client.cassandra.migration.service.CqlScriptParsingService;
import com.netcracker.cloud.dbaas.client.cassandra.migration.service.SchemaHistoryService;
import com.netcracker.cloud.dbaas.client.cassandra.migration.service.await.TableStateAwaitService;
import com.netcracker.cloud.dbaas.client.cassandra.migration.service.lock.MigrationLockService;
import com.netcracker.cloud.dbaas.client.cassandra.migration.session.SchemaMigrationSession;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.codehaus.plexus.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.netcracker.cloud.dbaas.client.cassandra.migration.SchemaMigrationCommonConstants.*;

@Slf4j
public class SchemaMigrationVersionProcessor {
    private static final Set<String> RETRYABLE_MESSAGES = Set.of(
            "is currently being created, altered or deleted",
            "unconfigured table"
    );

    private final CqlScriptParsingService scriptParsingService;
    private final SchemaHistoryService schemaHistoryService;
    private final TableStateAwaitService tableStateAwaitService;
    private final SchemaMigrationSession session;
    private final MigrationLockService migrationLockService;
    private final AmazonKeyspacesSettings amazonKeyspacesSettings;

    public SchemaMigrationVersionProcessor(
            SchemaMigrationSession session,
            SchemaHistoryService schemaHistoryService,
            MigrationLockService migrationLockService,
            SchemaMigrationSettings schemaMigrationSettings
    ) {
        this.session = session;
        this.schemaHistoryService = schemaHistoryService;
        this.amazonKeyspacesSettings = schemaMigrationSettings.amazonKeyspaces();
        this.scriptParsingService = new CqlScriptParsingService(schemaMigrationSettings);
        this.tableStateAwaitService = TableStateAwaitService.create(
                session, schemaMigrationSettings
        );
        this.migrationLockService = migrationLockService;
    }

    public void process(SchemaVersionToApply versionToApply) throws SchemaMigrationVersionProcessingException {
        log.info(MIGRATION_LOG_PREFIX + "Start applying schema version {}", versionToApply.version());

        // Flyway don't do previous insert, before .cql script will execute
        // But that could be problem with knowledge migration run/not run
        schemaHistoryService.insertPreProcessVersionData(versionToApply);
        SchemaVersionMigrationResult result = this.executeCqlStatements(versionToApply);
        schemaHistoryService.insertPostProcessVersionData(versionToApply, result);

        log.info(MIGRATION_LOG_PREFIX + "Stop applying schema version {}. Took {} ms", versionToApply.version(), result.executionTime());
        if (!result.success()) {
            throw new SchemaMigrationVersionProcessingException(result.exception());
        }
    }

    SchemaVersionMigrationResult executeCqlStatements(SchemaVersionToApply versionToApply) {
        long startTime = System.currentTimeMillis();
        try {
            List<String> statements = scriptParsingService.parseStatements(versionToApply.resourcePath(), versionToApply.script());
            log.info(MIGRATION_LOG_PREFIX + "Executing statements for schema version {}", versionToApply.version());

            for (String statement : statements) {
                executeStatementUntilSuccess(statement, versionToApply.settings().ignoreErrorPatterns());
            }

            this.tableStateAwaitService.await(versionToApply.settings().tableOperations());
            return new SchemaVersionMigrationResult(true, null, System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            return new SchemaVersionMigrationResult(false, e, System.currentTimeMillis() - startTime);
        }
    }

    private void executeStatementUntilSuccess(String statement, List<String> ignorePatterns) throws Exception {
        while (true) {
            if (!migrationLockService.isLockActive()) {
                throw new SchemaMigrationException(MIGRATION_LOG_PREFIX + "Unexpectedly lost cassandra lock, migration executing stopped");
            }

            try {
                session.execute(statement);
                return;
            } catch (DriverException e) {
                if (doRetryException(e)) {
                    Thread.sleep(500);
                    continue;
                } else if (isSupportedByIgnorePattern(e, ignorePatterns)) {
                    return;
                }

                throw e;
            }
        }
    }

    private boolean doRetryException(DriverException e) {
        //TODO should list of retryable messages be configurable
        if (
                amazonKeyspacesSettings.enabled()
                        && RETRYABLE_MESSAGES.stream().anyMatch(retryableMessage -> StringUtils.contains(e.getMessage(), retryableMessage))
        ) {
            log.warn(MIGRATION_LOG_PREFIX + "Exception caught while executing migration statement and will be retried", e);
            return true;
        }

        return false;
    }

    private boolean isSupportedByIgnorePattern(
            DriverException e, List<String> ignorePatterns
    ) {
        String exceptionMessage = e.getMessage();
        if (!CollectionUtils.isEmpty(ignorePatterns)) {
            Optional<String> patternOpt = ignorePatterns.stream()
                    .filter(exceptionMessage::matches)
                    .findFirst();
            if (patternOpt.isPresent()) {
                log.info(
                        MIGRATION_LOG_PREFIX + "DriverException with message \"{}\" ignored by pattern \"{}\"",
                        e.getMessage(), patternOpt.get()
                );
                return true;
            }
        }
        return false;
    }
}
