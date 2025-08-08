package org.qubership.cloud.dbaas.client.cassandra.migration;

import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ExecutionInfo;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Statement;
import com.datastax.oss.driver.api.core.metadata.Metadata;
import com.datastax.oss.driver.api.core.metadata.Node;
import com.datastax.oss.driver.api.core.metadata.schema.KeyspaceMetadata;
import com.datastax.oss.driver.api.core.servererrors.InvalidQueryException;
import org.qubership.cloud.dbaas.client.cassandra.migration.model.SchemaVersionFromResource;
import org.qubership.cloud.dbaas.client.cassandra.migration.model.SchemaVersionMigrationResult;
import org.qubership.cloud.dbaas.client.cassandra.migration.model.SchemaVersionToApply;
import org.qubership.cloud.dbaas.client.cassandra.migration.model.settings.SchemaMigrationSettings;
import org.qubership.cloud.dbaas.client.cassandra.migration.model.settings.ak.AmazonKeyspacesSettings;
import org.qubership.cloud.dbaas.client.cassandra.migration.service.SchemaHistoryService;
import org.qubership.cloud.dbaas.client.cassandra.migration.service.SchemaVersionResourceReaderImpl;
import org.qubership.cloud.dbaas.client.cassandra.migration.service.await.TableStateAwaitService;
import org.qubership.cloud.dbaas.client.cassandra.migration.service.lock.MigrationLockService;
import org.qubership.cloud.dbaas.client.cassandra.migration.service.resource.SchemaVersionResourceFinderRegistry;
import org.qubership.cloud.dbaas.client.cassandra.migration.session.SchemaMigrationSession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SchemaMigrationVersionProcessorTest {
    private List<SchemaVersionFromResource> allFileVersions;

    SchemaHistoryService schemaHistoryServiceMock = Mockito.mock(SchemaHistoryService.class);
    MigrationLockService migrationLockServiceMock = Mockito.mock(MigrationLockService.class);
    CqlSession mockedCqlSession = Mockito.mock(CqlSession.class);
    ExecutionInfo mockedExecutionInfo = mock(ExecutionInfo.class);
    Metadata mockedMetadata = mock(Metadata.class);
    CqlIdentifier cqlIdentifier = mock(CqlIdentifier.class);
    KeyspaceMetadata mockedKeyspaceMetadata = mock(KeyspaceMetadata.class);
    ResultSet mockedResultSet = mock(ResultSet.class);

    SchemaMigrationSession schemaMigrationSessionSpy;
    SchemaMigrationSettings schemaMigrationSettings;
    SchemaVersionResourceReaderImpl schemaVersionResourceAccessorDefault;
    SchemaMigrationVersionProcessor schemaMigrationVersionProcessor;

    @BeforeEach
    void setUp() {
        this.schemaMigrationSettings = SchemaMigrationSettings.builder()
                .withAmazonKeyspacesSettings(AmazonKeyspacesSettings.builder()
                        .enabled(true)
                        .build())
                .build();
        this.schemaVersionResourceAccessorDefault = new SchemaVersionResourceReaderImpl(
                schemaMigrationSettings.version(), new SchemaVersionResourceFinderRegistry()
        );
        this.schemaMigrationSessionSpy = spy(
                new SchemaMigrationSession(mockedCqlSession, schemaMigrationSettings.schemaAgreement())
        );
        Mockito.when(schemaMigrationSessionSpy.getKeyspace()).thenReturn(Optional.of(cqlIdentifier));
        Mockito.when(schemaMigrationSessionSpy.getMetadata()).thenReturn(mockedMetadata);
        Mockito.when(mockedMetadata.getKeyspace(eq(cqlIdentifier))).thenReturn(Optional.of(mockedKeyspaceMetadata));

        this.allFileVersions = schemaVersionResourceAccessorDefault.readSchemaVersionResources();

        try (MockedStatic<TableStateAwaitService> s = mockStatic(TableStateAwaitService.class)) {
            s.when(() -> TableStateAwaitService.create(schemaMigrationSessionSpy, schemaMigrationSettings))
                    .thenReturn(new TableStateAwaitService() {
                    });
            this.schemaMigrationVersionProcessor = new SchemaMigrationVersionProcessor(
                    schemaMigrationSessionSpy, schemaHistoryServiceMock,
                    migrationLockServiceMock, schemaMigrationSettings
            );
        }

        Mockito.when(mockedResultSet.getExecutionInfo()).thenReturn(mockedExecutionInfo);
        Mockito.when(mockedCqlSession.checkSchemaAgreement()).thenReturn(true);
        Mockito.when(mockedExecutionInfo.isSchemaInAgreement()).thenReturn(true);
        Mockito.when(migrationLockServiceMock.isLockActive()).thenReturn(true);
    }

    @Test
    void processorShouldMigrate_versionInAWSCase_eventIfThrows_retryableException() {
        InvalidQueryException firstRetryableException = new InvalidQueryException(
                mock(Node.class), "some table is currently being created, altered or deleted.");
        InvalidQueryException secondRetryableException = new InvalidQueryException(
                mock(Node.class), "unconfigured table");

        when(mockedCqlSession.execute(any(Statement.class)))
                .thenThrow(firstRetryableException)
                .thenThrow(secondRetryableException)
                .thenThrow(firstRetryableException)
                .thenReturn(mockedResultSet);

        SchemaVersionFromResource versionFromResource = allFileVersions.get(0);
        SchemaVersionToApply versionToApply = new SchemaVersionToApply(
                0, versionFromResource.version(), versionFromResource.description(), versionFromResource.type(), versionFromResource.resourcePath(),
                versionFromResource.script(), versionFromResource.checksum(), versionFromResource.settings()
        );

        SchemaVersionMigrationResult migrationResult = schemaMigrationVersionProcessor.executeCqlStatements(versionToApply);
        verify(mockedCqlSession, times(4)).execute(any(Statement.class));
        assertThat(migrationResult)
                .returns(true, SchemaVersionMigrationResult::success);
    }
}