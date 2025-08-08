package com.netcracker.cloud.dbaas.client.cassandra.migration.service.compatibility;

import com.netcracker.cloud.dbaas.client.cassandra.migration.model.AlreadyMigratedVersion;
import com.netcracker.cloud.dbaas.client.cassandra.migration.model.SchemaVersionFromDb;
import com.netcracker.cloud.dbaas.client.cassandra.migration.model.SchemaVersionFromResource;
import com.netcracker.cloud.dbaas.client.cassandra.migration.repository.SchemaHistoryRepository;
import com.netcracker.cloud.dbaas.client.cassandra.migration.service.extension.AlreadyMigratedVersionsExtensionPoint;
import com.netcracker.cloud.dbaas.client.cassandra.migration.session.SchemaMigrationSession;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.maven.artifact.versioning.ComparableVersion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.netcracker.cloud.dbaas.client.cassandra.migration.SchemaMigrationCommonConstants.HOST_NAME;
import static com.netcracker.cloud.dbaas.client.cassandra.migration.SchemaMigrationCommonConstants.MIGRATION_LOG_PREFIX;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

@Slf4j
public class AlreadyMigratedVersionsService {

    private final SchemaMigrationSession session;

    private final SchemaHistoryRepository schemaHistoryRepository;
    private final AlreadyMigratedVersionsExtensionPoint extensionPoint;

    public AlreadyMigratedVersionsService(
            SchemaMigrationSession session,
            SchemaHistoryRepository schemaHistoryRepository,
            AlreadyMigratedVersionsExtensionPoint extensionPoint
    ) {
        this.session = session;
        this.schemaHistoryRepository = schemaHistoryRepository;
        this.extensionPoint = extensionPoint;
    }

    public List<SchemaVersionFromDb> insertAlreadyMigratedVersions(List<SchemaVersionFromResource> versionsFromResources) {
        if (extensionPoint == null) {
            log.info(MIGRATION_LOG_PREFIX + "Already migrated versions extension point not provided.");
            return emptyList();
        }

        log.info(MIGRATION_LOG_PREFIX + "Start inserting already migrated versions.");
        Map<ComparableVersion, AlreadyMigratedVersion> alreadyMigratedVersions = getAlreadyMigratedVersionsFromExtensionPoint();

        if (MapUtils.isEmpty(alreadyMigratedVersions)) {
            log.info(MIGRATION_LOG_PREFIX + "Stop inserting already migrated versions. No versions provided by extension point.");
            return emptyList();
        }

        int installedRank = 1;
        List<SchemaVersionFromDb> result = new ArrayList<>();
        for (SchemaVersionFromResource versionFromResource : versionsFromResources) {
            AlreadyMigratedVersion alreadyMigratedVersion = alreadyMigratedVersions.get(versionFromResource.version());
            if (alreadyMigratedVersion != null) {
                SchemaVersionFromDb dbVersion = new SchemaVersionFromDb(
                        versionFromResource.version(), versionFromResource.checksum(), alreadyMigratedVersion.success(),
                        installedRank++, versionFromResource.description()
                );
                log.info(
                        MIGRATION_LOG_PREFIX + "Inserting version {} as already migrated with rank {} success {}.",
                        dbVersion.version(), dbVersion.installedRank(), dbVersion.success()
                );
                schemaHistoryRepository.insert(
                        dbVersion.installedRank(), dbVersion.version().toString(), dbVersion.description(), versionFromResource.type(),
                        versionFromResource.resourcePath(), dbVersion.checksum(), HOST_NAME, dbVersion.success(),
                        "Provided by extension point as already migrated"
                );
                result.add(dbVersion);
            }
        }

        log.info(MIGRATION_LOG_PREFIX + "Stop inserting already migrated versions.");
        return result;
    }

    private Map<ComparableVersion, AlreadyMigratedVersion> getAlreadyMigratedVersionsFromExtensionPoint() {
        List<AlreadyMigratedVersion> alreadyMigratedVersions = extensionPoint.getAlreadyMigratedVersions(session.getSession());
        log.info(MIGRATION_LOG_PREFIX + "Extension point provided already migrated versions {}", alreadyMigratedVersions);
        if (CollectionUtils.isEmpty(alreadyMigratedVersions)) {
            return emptyMap();
        }

        return alreadyMigratedVersions.stream()
                .collect(Collectors.toMap(
                        version -> new ComparableVersion(version.version()),
                        Function.identity()
                ));
    }
}
