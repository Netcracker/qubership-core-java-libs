package com.netcracker.cloud.dbaas.client.cassandra.migration.service;


import com.netcracker.cloud.dbaas.client.cassandra.migration.model.SchemaVersionFromResource;
import com.netcracker.cloud.dbaas.client.cassandra.migration.model.settings.SchemaMigrationSettings;
import com.netcracker.cloud.dbaas.client.cassandra.migration.model.settings.ak.AmazonKeyspacesSettings;
import com.netcracker.cloud.dbaas.client.cassandra.migration.service.resource.SchemaVersionResourceFinderRegistry;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.netcracker.cloud.dbaas.client.cassandra.migration.util.ResourceUtils.FTL_EXTENSION;

class CqlScriptParsingServiceTest {

    @Test
    void parseStatement_shouldAdd_AWSFunctions_butNotCassandra_ifIsAmazonKeyspacesTrue() {
        SchemaMigrationSettings schemaMigrationSettings = SchemaMigrationSettings.builder()
                .withAmazonKeyspacesSettings(AmazonKeyspacesSettings.builder()
                        .enabled(true)
                        .build())
                .build();
        CqlScriptParsingService cqlScriptParsingService = new CqlScriptParsingService(schemaMigrationSettings);
        SchemaVersionResourceReaderImpl resourceReader = new SchemaVersionResourceReaderImpl(
                schemaMigrationSettings.version(), new SchemaVersionResourceFinderRegistry()
        );

        List<SchemaVersionFromResource> schemaVersionFtlResources = resourceReader.readSchemaVersionResources()
                .stream()
                .filter(s -> s.resourcePath().endsWith(FTL_EXTENSION))
                .toList();

        Assertions.assertTrue(CollectionUtils.isNotEmpty(schemaVersionFtlResources));

        List<String> allParsedStatements = new ArrayList<>();
        for (SchemaVersionFromResource versionFromResource : schemaVersionFtlResources) {
            List<String> parsedStatements = cqlScriptParsingService.parseStatements(
                    versionFromResource.resourcePath(), versionFromResource.script()
            );
            allParsedStatements.addAll(parsedStatements);
        }

        Assertions.assertTrue(
                allParsedStatements.stream().anyMatch(parsedStatement -> parsedStatement.contains("WITH custom_properties"))
        );
        Assertions.assertTrue(
                allParsedStatements.stream().noneMatch(parsedStatement -> parsedStatement.contains("with compaction"))
        );
    }

    @Test
    void parseStatement_shouldAdd_CassandraFunctions_butNotAWS_ifIsAmazonKeyspacesFalse() {
        SchemaMigrationSettings schemaMigrationSettings = SchemaMigrationSettings.builder()
                .withAmazonKeyspacesSettings(AmazonKeyspacesSettings.builder()
                        .enabled(false)
                        .build()).build();
        CqlScriptParsingService cqlScriptParsingService = new CqlScriptParsingService(schemaMigrationSettings);
        SchemaVersionResourceReaderImpl resourceReader = new SchemaVersionResourceReaderImpl(
                schemaMigrationSettings.version(), new SchemaVersionResourceFinderRegistry()
        );

        List<SchemaVersionFromResource> schemaVersionFtlResources = resourceReader.readSchemaVersionResources()
                .stream()
                .filter(s -> s.resourcePath().endsWith(FTL_EXTENSION))
                .toList();

        Assertions.assertTrue(CollectionUtils.isNotEmpty(schemaVersionFtlResources));

        List<String> allParsedStatements = new ArrayList<>();
        for (SchemaVersionFromResource versionFromResource : schemaVersionFtlResources) {
            List<String> parsedStatements = cqlScriptParsingService.parseStatements(
                    versionFromResource.resourcePath(), versionFromResource.script()
            );
            allParsedStatements.addAll(parsedStatements);
        }

        Assertions.assertTrue(
                allParsedStatements.stream().noneMatch(parsedStatement -> parsedStatement.contains("WITH custom_properties"))
        );
        Assertions.assertTrue(
                allParsedStatements.stream().anyMatch(parsedStatement -> parsedStatement.contains("with compaction"))
        );
    }
}
