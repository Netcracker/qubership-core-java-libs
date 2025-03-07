package org.qubership.cloud.dbaas.client.cassandra.migration.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.util.Optional;

import static java.util.Collections.emptyMap;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ResourceUtils {
    public static final String CQL_EXTENSION = ".cql";
    public static final String FTL_EXTENSION = CQL_EXTENSION + ".ftl";

    public static ClassLoader getResourceClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    public static InputStream openInputStreamForResource(String resourcePath) {
        return getResourceClassLoader().getResourceAsStream(resourcePath);
    }

    public static FileSystem getFileSystem(URI location) throws IOException {
        try {
            log.debug("Trying to get existing filesystem for {}", location.toString());
            return FileSystems.getFileSystem(location);
        } catch (FileSystemNotFoundException exception) {
            log.debug("Creating new filesystem for {}", location);
            return FileSystems.newFileSystem(location, emptyMap());
        }
    }

    public static boolean hasVersionScriptExtension(String fileName) {
        return fileName.endsWith(CQL_EXTENSION) || fileName.endsWith(FTL_EXTENSION);
    }
}
