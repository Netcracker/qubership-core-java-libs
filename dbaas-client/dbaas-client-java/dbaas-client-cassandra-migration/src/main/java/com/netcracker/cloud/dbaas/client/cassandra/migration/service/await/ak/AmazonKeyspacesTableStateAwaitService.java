package com.netcracker.cloud.dbaas.client.cassandra.migration.service.await.ak;

import com.netcracker.cloud.dbaas.client.cassandra.migration.exception.SchemaMigrationException;
import com.netcracker.cloud.dbaas.client.cassandra.migration.model.operation.TableOperation;
import com.netcracker.cloud.dbaas.client.cassandra.migration.model.settings.ak.TableStatusCheckSettings;
import com.netcracker.cloud.dbaas.client.cassandra.migration.repository.AmazonKeyspacesMcsTablesRepository;
import com.netcracker.cloud.dbaas.client.cassandra.migration.service.await.TableStateAwaitService;
import com.netcracker.cloud.dbaas.client.cassandra.migration.session.SchemaMigrationSession;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.netcracker.cloud.dbaas.client.cassandra.migration.SchemaMigrationCommonConstants.MIGRATION_LOG_PREFIX;

@Slf4j
public class AmazonKeyspacesTableStateAwaitService implements TableStateAwaitService {
    private final TableStatusCheckSettings settings;
    private final AmazonKeyspacesMcsTablesRepository tableStatusRepository;

    public AmazonKeyspacesTableStateAwaitService(
            SchemaMigrationSession session,
            TableStatusCheckSettings settings
    ) {
        this.settings = settings;
        this.tableStatusRepository = new AmazonKeyspacesMcsTablesRepository(session);
    }

    @Override
    public void await(List<TableOperation> tableOperations) {
        if (CollectionUtils.isEmpty(tableOperations)) {
            return;
        }
        List<ExpectedOperationResult> expectedOperationResults = tableOperations.stream()
                .map(tableOperation -> new ExpectedOperationResult(
                        tableOperation.tableName(),
                        ExpectedTableStatus.resolve(tableOperation.operationType()),
                        null
                ))
                .toList();
        awaitInternal(expectedOperationResults);
    }

    private void awaitInternal(
            List<ExpectedOperationResult> expectedOperationResults
    ) {
        log.info(MIGRATION_LOG_PREFIX + "Waiting for expected operation result begins: {}", expectedOperationResults);
        try {
            Thread.sleep(settings.preDelay());

            List<ExpectedOperationResult> waitingExpectedOperationResult = expectedOperationResults;
            while (true) {
                waitingExpectedOperationResult = recalculateExpectedOperationResult(waitingExpectedOperationResult);
                if (CollectionUtils.isEmpty(waitingExpectedOperationResult)) {
                    log.info(MIGRATION_LOG_PREFIX + "Waiting for expected operation result completed");
                    return;
                }
                log.info(MIGRATION_LOG_PREFIX + "Waiting for expected operation result continues: {}", waitingExpectedOperationResult);
                Thread.sleep(settings.retryDelay());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SchemaMigrationException("Interrupted while waiting for expected operation", e);
        }
    }

    private List<ExpectedOperationResult> recalculateExpectedOperationResult(
            List<ExpectedOperationResult> previousExpectedResult
    ) {
        Map<String, String> tableNameToStatusFromDB = tableStatusRepository.getAllTableStatuses();
        return previousExpectedResult.stream()
                .map(result -> calculateExpectedOperationResult(result, tableNameToStatusFromDB.get(result.tableName())))
                .filter(Objects::nonNull)
                .toList();
    }

    private ExpectedOperationResult calculateExpectedOperationResult(
            ExpectedOperationResult expectedResult, String newStatusFromDB
    ) {
        if (isOperationResultAchieved(expectedResult, newStatusFromDB)) {
            return null;
        }

        return new ExpectedOperationResult(expectedResult.tableName(), expectedResult.status(), newStatusFromDB);
    }

    private boolean isOperationResultAchieved(
            ExpectedOperationResult expectedResult, String newStatusFromDB
    ) {
        if (expectedResult.status() == ExpectedTableStatus.NOT_EXISTS) {
            return newStatusFromDB == null;
        } else {
            return ExpectedTableStatus.isPresent(newStatusFromDB)
                    && ExpectedTableStatus.valueOf(newStatusFromDB) == expectedResult.status();
        }
    }
}
