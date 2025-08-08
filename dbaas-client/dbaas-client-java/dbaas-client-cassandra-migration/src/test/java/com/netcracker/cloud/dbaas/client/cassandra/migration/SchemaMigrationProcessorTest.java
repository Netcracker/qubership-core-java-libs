package com.netcracker.cloud.dbaas.client.cassandra.migration;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ExecutionInfo;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Statement;
import com.netcracker.cloud.dbaas.client.cassandra.migration.model.SchemaVersionFromDb;
import com.netcracker.cloud.dbaas.client.cassandra.migration.model.SchemaVersionFromResource;
import com.netcracker.cloud.dbaas.client.cassandra.migration.model.SchemaVersionToApply;
import com.netcracker.cloud.dbaas.client.cassandra.migration.model.settings.SchemaMigrationSettings;
import com.netcracker.cloud.dbaas.client.cassandra.migration.model.settings.ak.AmazonKeyspacesSettings;
import com.netcracker.cloud.dbaas.client.cassandra.migration.service.SchemaHistoryService;
import com.netcracker.cloud.dbaas.client.cassandra.migration.service.SchemaVersionResourceReaderImpl;
import com.netcracker.cloud.dbaas.client.cassandra.migration.service.compatibility.AlreadyMigratedVersionsService;
import com.netcracker.cloud.dbaas.client.cassandra.migration.service.lock.MigrationLockService;
import com.netcracker.cloud.dbaas.client.cassandra.migration.service.resource.SchemaVersionResourceFinderRegistry;
import com.netcracker.cloud.dbaas.client.cassandra.migration.session.SchemaMigrationSession;
import com.netcracker.cloud.dbaas.client.cassandra.migration.util.ChecksumUtils;
import lombok.SneakyThrows;

import org.assertj.core.api.ThrowingConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;


public class SchemaMigrationProcessorTest {
    SchemaMigrationSession schemaMigrationSessionSpy;
    private SchemaMigrationProcessor schemaMigrationProcessor;
    private SchemaMigrationVersionProcessor schemaMigrationVersionProcessor;
    private List<SchemaVersionFromResource> allActualFileVersions;

    CqlSession mockedCqlSession = Mockito.mock(CqlSession.class);
    SchemaHistoryService schemaHistoryServiceMock = Mockito.mock(SchemaHistoryService.class);
    MigrationLockService migrationLockServiceMock = Mockito.mock(MigrationLockService.class);
    AlreadyMigratedVersionsService alreadyMigratedVersionsServiceMock = Mockito.mock(AlreadyMigratedVersionsService.class);
    ResultSet mockedResultSet = mock(ResultSet.class);
    ExecutionInfo mockedExecutionInfo = mock(ExecutionInfo.class);

    @BeforeEach
    public void setUp() throws Exception {
        SchemaMigrationSettings schemaMigrationSettings = SchemaMigrationSettings.builder()
                .withAmazonKeyspacesSettings(AmazonKeyspacesSettings.builder()
                        .enabled(false)
                        .build())
                .build();

        SchemaVersionResourceReaderImpl schemaVersionResourceAccessorDefault = new SchemaVersionResourceReaderImpl(
                schemaMigrationSettings.version(), new SchemaVersionResourceFinderRegistry()
        );
        this.schemaMigrationSessionSpy = spy(new SchemaMigrationSession(mockedCqlSession, schemaMigrationSettings.schemaAgreement()));
        this.schemaMigrationVersionProcessor = spy(new SchemaMigrationVersionProcessor(
                schemaMigrationSessionSpy, schemaHistoryServiceMock,
                migrationLockServiceMock, schemaMigrationSettings
        ));
        this.schemaMigrationProcessor = spy(new SchemaMigrationProcessor(
                migrationLockServiceMock, schemaHistoryServiceMock, schemaVersionResourceAccessorDefault,
                schemaMigrationVersionProcessor, alreadyMigratedVersionsServiceMock
        ));
        this.allActualFileVersions = schemaVersionResourceAccessorDefault.readSchemaVersionResources();

        Mockito.when(schemaHistoryServiceMock.getSchemaVersionsFromDb()).thenReturn(Collections.emptyList());
        Mockito.when(migrationLockServiceMock.isLockActive()).thenReturn(true);
        Mockito.when(mockedCqlSession.execute(any(String.class))).thenReturn(mockedResultSet);
        Mockito.when(mockedCqlSession.execute(any(Statement.class))).thenReturn(mockedResultSet);
        Mockito.when(mockedResultSet.getExecutionInfo()).thenReturn(mockedExecutionInfo);
        Mockito.when(mockedCqlSession.checkSchemaAgreement()).thenReturn(true);
        Mockito.when(mockedExecutionInfo.isSchemaInAgreement()).thenReturn(true);
    }

    private long invalidateChecksum(long sourceChecksum) {
        return sourceChecksum + 1;
    }

    private SchemaVersionFromDb createDbSchemaVersionFromResource(
            SchemaVersionFromResource versionFromResource, int installedRank,
            Collection<String> checksumInvalidationVersions, Collection<String> failedVersions
    ) {
        String versionStr = versionFromResource.version().toString();
        long checksum = ChecksumUtils.readContentAndCalculateChecksum(
                versionFromResource.resourcePath()
        ).checksum();
        if (checksumInvalidationVersions != null && checksumInvalidationVersions.contains(versionStr)) {
            checksum = invalidateChecksum(checksum);
        }
        boolean success = failedVersions == null || !failedVersions.contains(versionStr);
        return new SchemaVersionFromDb(
                versionFromResource.version(), checksum, success, installedRank, versionFromResource.description()
        );
    }

    //dbSchemaVersions == null - all
    private List<SchemaVersionFromDb> createDbSchemaVersionsFromResources(
            Collection<String> dbSchemaVersions, Collection<String> checksumInvalidationVersions, Collection<String> failedVersions
    ) {
        AtomicInteger rank = new AtomicInteger(0);
        return allActualFileVersions.stream()
                .filter(version -> dbSchemaVersions == null || dbSchemaVersions.contains(version.version().toString()))
                .map(version -> createDbSchemaVersionFromResource(
                        version, rank.getAndIncrement(), checksumInvalidationVersions, failedVersions
                ))
                .toList();
    }

    @Test
    @SneakyThrows
    public void processorShouldNotMigrate_anyVersion_ifDBAlreadyMigrated() {
        executeMigration(null, null, null);

        Mockito.verify(schemaMigrationVersionProcessor, Mockito.never()).process(ArgumentMatchers.any());
    }

    @Test
    public void processorShouldReapply_ifPreviousStateIsInvalid() {
        String testVersion = "1.0.0.0_01";
        ArgumentCaptor<SchemaVersionToApply> schemaVersionToApplyCaptor = ArgumentCaptor.forClass(SchemaVersionToApply.class);

        SchemaVersionFromResource versionWithInvalidState = allActualFileVersions.stream()
                .filter(version -> testVersion.equals(version.version().toString()))
                .findFirst().orElseThrow();

        executeMigration(null, Set.of(testVersion), null);

        Mockito.verify(schemaMigrationVersionProcessor, times(1))
                .executeCqlStatements(schemaVersionToApplyCaptor.capture());

        assertThat(schemaVersionToApplyCaptor.getValue())
                .returns(versionWithInvalidState.version(), SchemaVersionToApply::version)
                .returns(versionWithInvalidState.checksum(), SchemaVersionToApply::checksum)
                .satisfies(versionToApply -> assertThat(versionToApply.description())
                        .contains("Checksum updated")
                        .contains(String.valueOf(versionToApply.checksum()))
                        .contains(String.valueOf(invalidateChecksum(versionToApply.checksum())))
                );
    }

    @Test
    public void processorShouldSkip_ifPreviousStateIsValid() {
        executeMigration(null, Set.of("1.0.0.0_02"), null);
        Mockito.verify(schemaMigrationVersionProcessor, times(0)).executeCqlStatements(any());
    }

    private void executeMigration(
            Collection<String> dbSchemaVersions, Collection<String> checksumInvalidationVersions, Collection<String> failedVersions
    ) {
        List<SchemaVersionFromDb> versionsFromDb = createDbSchemaVersionsFromResources(dbSchemaVersions, checksumInvalidationVersions, failedVersions);
        Mockito.when(schemaHistoryServiceMock.getSchemaVersionsFromDb()).thenReturn(versionsFromDb);

        schemaMigrationProcessor.migrateIfNeeded();
    }

    private ThrowingConsumer<SchemaVersionToApply> assertAppliedSchemaVersionEqualToResource(
            SchemaVersionFromResource versionFromResource
    ) {
        return versionToApply -> assertThat(versionToApply)
                .returns(versionFromResource.version(), SchemaVersionToApply::version)
                .returns(versionFromResource.description(), SchemaVersionToApply::description)
                .returns(versionFromResource.type(), SchemaVersionToApply::type)
                .returns(versionFromResource.resourcePath(), SchemaVersionToApply::resourcePath)
                .returns(versionFromResource.script(), SchemaVersionToApply::script)
                .returns(versionFromResource.checksum(), SchemaVersionToApply::checksum);
    }

    @Test
    public void processorShouldMigrate_allVersions_IfDBNeverMigratedBefore() {
        ArgumentCaptor<SchemaVersionToApply> insertPreProcessCapture = ArgumentCaptor.forClass(SchemaVersionToApply.class);
        ArgumentCaptor<SchemaVersionToApply> insertPostProcessCapture = ArgumentCaptor.forClass(SchemaVersionToApply.class);
        ArgumentCaptor<SchemaVersionToApply> executeStatementsCaptor = ArgumentCaptor.forClass(SchemaVersionToApply.class);

        executeMigration(Set.of(), null, null);

        Mockito.verify(schemaHistoryServiceMock, times(allActualFileVersions.size()))
                .insertPreProcessVersionData(insertPreProcessCapture.capture());
        Mockito.verify(schemaHistoryServiceMock, times(allActualFileVersions.size()))
                .insertPostProcessVersionData(insertPostProcessCapture.capture(), ArgumentMatchers.any());
        Mockito.verify(schemaMigrationVersionProcessor, times(allActualFileVersions.size()))
                .executeCqlStatements(executeStatementsCaptor.capture());

        List<SchemaVersionToApply> preProcessVersions = insertPreProcessCapture.getAllValues();
        List<SchemaVersionToApply> postProcessVersions = insertPostProcessCapture.getAllValues();
        List<SchemaVersionToApply> executeStatementsVersions = insertPreProcessCapture.getAllValues();

        for (int i = 0; i < preProcessVersions.size(); i++) {
            SchemaVersionToApply versionToApply = executeStatementsVersions.get(i);
            assertThat(versionToApply)
                    .isEqualTo(preProcessVersions.get(i))
                    .isEqualTo(postProcessVersions.get(i))
                    .satisfies(this.assertAppliedSchemaVersionEqualToResource(allActualFileVersions.get(i)));
        }
    }

    @Test
    public void processorShouldMigrate_versionsPartially_evenIfMigrationFailBefore_ignoreOrder() {
        String notInstalledVersion = "1.0.0.0_01";
        Set<String> failedVersions = Set.of("1.0.0.0_02", "2.0.0.0_01");

        ArgumentCaptor<SchemaVersionToApply> executeStatementsCaptor = ArgumentCaptor.forClass(SchemaVersionToApply.class);
        executeMigration(
                allActualFileVersions.stream()
                        .map(version -> version.version().toString())
                        .filter(version -> !notInstalledVersion.equals(version))
                        .collect(Collectors.toSet()),
                null, failedVersions
        );
        Mockito.verify(schemaMigrationVersionProcessor, times(3))
                .executeCqlStatements(executeStatementsCaptor.capture());

        List<SchemaVersionToApply> executeStatementsVersions = executeStatementsCaptor.getAllValues();
        int i = 0;
        for (SchemaVersionFromResource versionFromResource : allActualFileVersions) {
            String versionStr = versionFromResource.version().toString();
            if (notInstalledVersion.equals(versionStr) || failedVersions.contains(versionStr)) {
                assertThat(executeStatementsVersions.get(i++))
                        .satisfies(this.assertAppliedSchemaVersionEqualToResource(versionFromResource));
            }
        }
    }
}
