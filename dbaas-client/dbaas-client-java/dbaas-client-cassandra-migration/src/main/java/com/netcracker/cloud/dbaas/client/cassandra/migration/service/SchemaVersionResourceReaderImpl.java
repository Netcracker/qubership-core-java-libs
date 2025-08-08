package com.netcracker.cloud.dbaas.client.cassandra.migration.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netcracker.cloud.dbaas.client.cassandra.migration.exception.SchemaMigrationException;
import com.netcracker.cloud.dbaas.client.cassandra.migration.model.SchemaVersionContentChecksum;
import com.netcracker.cloud.dbaas.client.cassandra.migration.model.SchemaVersionFromResource;
import com.netcracker.cloud.dbaas.client.cassandra.migration.model.SchemaVersionSettings;
import com.netcracker.cloud.dbaas.client.cassandra.migration.model.settings.VersionSettings;
import com.netcracker.cloud.dbaas.client.cassandra.migration.service.resource.SchemaVersionResourceFinder;
import com.netcracker.cloud.dbaas.client.cassandra.migration.service.resource.SchemaVersionResourceFinderRegistry;
import com.netcracker.cloud.dbaas.client.cassandra.migration.util.ResourceUtils;
import lombok.extern.slf4j.Slf4j;

import org.apache.maven.artifact.versioning.ComparableVersion;
import com.netcracker.cloud.dbaas.client.cassandra.migration.util.ChecksumUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.emptyMap;
import static com.netcracker.cloud.dbaas.client.cassandra.migration.SchemaMigrationCommonConstants.*;

/**
 * Common resource reader implementation.
 * Allows only .cql and .cql.ftl resource types
 */
@Slf4j
public class SchemaVersionResourceReaderImpl implements SchemaVersionResourceReader {
    private final Pattern versionResourceNamePattern;
    private final String versionDirectoryPath;
    private final String settingsResourcePath;

    private final ObjectMapper objectMapper;
    private final SchemaVersionResourceFinderRegistry finderRegistry;

    public SchemaVersionResourceReaderImpl(
            VersionSettings versionSettings,
            SchemaVersionResourceFinderRegistry finderRegistry
    ) {
        this.objectMapper = new ObjectMapper();
        this.finderRegistry = finderRegistry;
        this.versionResourceNamePattern = Pattern.compile(versionSettings.resourceNamePattern());
        this.versionDirectoryPath = versionSettings.directoryPath();
        this.settingsResourcePath = versionSettings.settingsResourcePath();
    }

    public List<SchemaVersionFromResource> readSchemaVersionResources() {
        log.info(MIGRATION_LOG_PREFIX + "Searching schema version resources in {}", versionDirectoryPath);
        Map<String, SchemaVersionSettings> settingsMap = readSettings();

        List<String> foundResourceNames = new ArrayList<>();
        for (URI resourceHolderUri : getResourceHolderUriList()) {
            log.info(MIGRATION_LOG_PREFIX + "Reading schema version resources from {}", resourceHolderUri);

            List<String> resourceNames = getResourceNames(resourceHolderUri);
            log.info(MIGRATION_LOG_PREFIX + "Found schema version resource names {}", resourceNames);
            foundResourceNames.addAll(resourceNames);
        }

        List<SchemaVersionFromResource> foundSchemaVersions = foundResourceNames.stream()
                .map(resourceName -> createSchemaVersionFromResourceName(resourceName, settingsMap))
                .sorted(Comparator.comparing(SchemaVersionFromResource::version))
                .toList();

        log.info(MIGRATION_LOG_PREFIX + "Found schema versions {}", foundSchemaVersions.stream().map(SchemaVersionFromResource::version).toList());

        return foundSchemaVersions;
    }

    private List<URI> getResourceHolderUriList() {
        try {
            List<URI> result = new ArrayList<>();
            Iterator<URL> iter = ResourceUtils.getResourceClassLoader().getResources(versionDirectoryPath).asIterator();
            while (iter.hasNext()) {
                result.add(iter.next().toURI());
            }
            return result;
        } catch (IOException e) {
            String msg = String.format("Unable to search for schema version resources in %s", versionDirectoryPath);
            log.error(MIGRATION_LOG_PREFIX + msg, e);
            throw new SchemaMigrationException(msg, e);
        } catch (URISyntaxException e) {
            String msg = "Unable to convert schema version resource holder to URI";
            log.error(MIGRATION_LOG_PREFIX + msg, e);
            throw new SchemaMigrationException(msg, e);
        }
    }

    private SchemaVersionResourceFinder getResourceFinder(URI resourceHolderUri) {
        SchemaVersionResourceFinder resourceFinder = finderRegistry.getFinder(resourceHolderUri.getScheme());
        if (resourceFinder == null) {
            throw new IllegalArgumentException(String.format(
                    "Schema version resource finder for %s not found", resourceHolderUri
            ));
        }
        return resourceFinder;
    }

    private List<String> getResourceNamesFromFinder(URI resourceHolderUri) {
        SchemaVersionResourceFinder resourceFinder = getResourceFinder(resourceHolderUri);
        try {
            return resourceFinder.findResourceNames(resourceHolderUri, versionDirectoryPath);
        } catch (IOException e) {
            String msg = String.format(
                    "Unable to find schema version resource names for %s in %s",
                    versionDirectoryPath, resourceHolderUri
            );
            log.error(MIGRATION_LOG_PREFIX + msg, e);
            throw new SchemaMigrationException(msg, e);
        }
    }

    private List<String> getResourceNames(URI resourceHolderUri) {
        return getResourceNamesFromFinder(resourceHolderUri).stream().filter(resourceName -> {
            boolean result = ResourceUtils.hasVersionScriptExtension(resourceName);
            if (!result) {
                log.warn(MIGRATION_LOG_PREFIX + "Skipping resource {} as having unexpected extension", resourceName);
            }
            return result;
        }).toList();
    }

    private Matcher createMatcherForResourceName(String resourceName) {
        Matcher matcher = versionResourceNamePattern.matcher(resourceName);

        if (!matcher.find()) {
            throw new IllegalArgumentException(String.format(
                    "Version file name %s does not match pattern %s provided in settings",
                    resourceName, versionResourceNamePattern
            ));
        }
        return matcher;
    }

    private SchemaVersionFromResource createSchemaVersionFromResourceName(
            String resourceName, Map<String, SchemaVersionSettings> settingsMap
    ) {
        Matcher m = createMatcherForResourceName(resourceName);
        String versionAsString = m.group(1);
        String resourcePath = versionDirectoryPath + DEFAULT_RESOURCE_SEPARATOR + resourceName;
        SchemaVersionContentChecksum contentAndChecksum =
                ChecksumUtils.readContentAndCalculateChecksum(resourcePath);
        return new SchemaVersionFromResource(
                new ComparableVersion(versionAsString), versionAsString, m.group(2), m.group(3),
                resourcePath, contentAndChecksum.versionContent(),
                contentAndChecksum.checksum(), settingsMap.get(versionAsString)
        );
    }

    Map<String, SchemaVersionSettings> readSettings() {
        log.info(MIGRATION_LOG_PREFIX + "Reading schema version settings from {}", settingsResourcePath);
        Map<String, SchemaVersionSettings> settingsMap;
        try (InputStream is = ResourceUtils.openInputStreamForResource(settingsResourcePath)) {
            if (is == null) {
                return emptyMap();
            }
            settingsMap = Optional.ofNullable(
                    objectMapper.readValue(is, new TypeReference<Map<String, SchemaVersionSettings>>() {
                    })
            ).orElse(emptyMap());
        } catch (IOException e) {
            throw new SchemaMigrationException("Failed to read schema version settings from " + settingsResourcePath, e);
        }

        log.info(MIGRATION_LOG_PREFIX + "Schema version settings read for versions {}", settingsMap.keySet());
        return settingsMap;
    }
}
