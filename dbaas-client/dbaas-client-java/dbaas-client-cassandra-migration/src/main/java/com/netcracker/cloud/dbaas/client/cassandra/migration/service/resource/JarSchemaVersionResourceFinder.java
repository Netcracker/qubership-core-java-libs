package org.qubership.cloud.dbaas.client.cassandra.migration.service.resource;

import org.qubership.cloud.dbaas.client.cassandra.migration.util.ResourceUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
public class JarSchemaVersionResourceFinder implements SchemaVersionResourceFinder {
    @Override
    public List<String> findResourceNames(URI resourceHolderUri, String resourceLocation) throws IOException {
        try (FileSystem fileSystem = ResourceUtils.getFileSystem(resourceHolderUri)) {
            final Path systemPath = fileSystem.getPath(resourceLocation);
            try (Stream<Path> fileWalksStream = Files.walk(systemPath)) {
                return fileWalksStream.filter(Files::isRegularFile)
                        .map(path -> path.getFileName().toString())
                        .toList();
            }
        }
    }
}
