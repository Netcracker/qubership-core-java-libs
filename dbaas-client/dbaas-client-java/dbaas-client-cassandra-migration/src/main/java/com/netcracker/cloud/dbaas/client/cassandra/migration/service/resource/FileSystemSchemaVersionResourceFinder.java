package org.qubership.cloud.dbaas.client.cassandra.migration.service.resource;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.qubership.cloud.dbaas.client.cassandra.migration.SchemaMigrationCommonConstants.DEFAULT_RESOURCE_SEPARATOR;
import static org.qubership.cloud.dbaas.client.cassandra.migration.SchemaMigrationCommonConstants.MIGRATION_LOG_PREFIX;
import static java.net.URLDecoder.decode;
import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
public class FileSystemSchemaVersionResourceFinder implements SchemaVersionResourceFinder {
    @Override
    public List<String> findResourceNames(URI resourceHolderUri, String resourceLocation) {
        String filePath = toFilePath(resourceHolderUri);
        File folder = new File(filePath);
        if (!folder.isDirectory()) {
            log.debug(MIGRATION_LOG_PREFIX + "Skipping path as it is not a directory: {}", filePath);
            return Collections.emptyList();
        }

        String rootClassPath = filePath.substring(0, filePath.length() - resourceLocation.length());
        if (!rootClassPath.endsWith(DEFAULT_RESOURCE_SEPARATOR)) {
            rootClassPath = rootClassPath + DEFAULT_RESOURCE_SEPARATOR;
        }
        log.debug(MIGRATION_LOG_PREFIX + "Walking start at classpath root in filesystem: {}", rootClassPath);
        return walkResourceNames(resourceLocation, folder);
    }

    private static String toFilePath(URI uri) {
        String filePath = new File(decode(uri.getPath().replace("+", "%2b"), UTF_8)).getAbsolutePath();
        if (filePath.endsWith("/")) {
            return filePath.substring(0, filePath.length() - 1);
        }
        return filePath;
    }

    private List<String> walkResourceNames(
            String scanRootLocation,
            File folder
    ) {
        log.debug("Scanning for resources in path: {} ({})", folder.getPath(), scanRootLocation);

        File[] files = folder.listFiles();
        if (files == null) {
            return Collections.emptyList();
        }

        List<String> resourceNames = new ArrayList<>();

        for (File file : files) {
            if (file.canRead()) {
                if (file.isDirectory()) {
                    resourceNames.addAll(walkResourceNames(scanRootLocation, file));
                } else {
                    resourceNames.add(file.getName());
                }
            }
        }

        return resourceNames;
    }
}
