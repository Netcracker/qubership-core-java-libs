package com.netcracker.cloud.dbaas.client.cassandra.migration.service;


import org.qubership.cloud.dbaas.client.cassandra.migration.model.SchemaVersionFromResource;
import org.qubership.cloud.dbaas.client.cassandra.migration.model.settings.SchemaMigrationSettings;
import org.qubership.cloud.dbaas.client.cassandra.migration.model.settings.ak.AmazonKeyspacesSettings;
import org.qubership.cloud.dbaas.client.cassandra.migration.service.resource.SchemaVersionResourceFinderRegistry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SchemaVersionResourceReaderImplTest {
    private SchemaVersionResourceReaderImpl resourceReader;

    @BeforeEach
    void setUp() {
        SchemaMigrationSettings schemaMigrationSettings = SchemaMigrationSettings.builder()
                .withAmazonKeyspacesSettings(AmazonKeyspacesSettings.builder()
                        .enabled(false)
                        .build())
                .build();
        this.resourceReader = new SchemaVersionResourceReaderImpl(
                schemaMigrationSettings.version(), new SchemaVersionResourceFinderRegistry()
        );
    }

    @Test
    public void migrationDataAccess_shouldReadCorrectVersionFiles_FromDefaultStorageFolder() {
        //TODO add proper expected result validation, think about using separate resources for tests

        List<SchemaVersionFromResource> versionsFromResources = resourceReader.readSchemaVersionResources();

        assertThat(versionsFromResources)
                .isNotEmpty()
                .allSatisfy(version -> assertThat(version).hasNoNullFieldsOrProperties());
    }
}
