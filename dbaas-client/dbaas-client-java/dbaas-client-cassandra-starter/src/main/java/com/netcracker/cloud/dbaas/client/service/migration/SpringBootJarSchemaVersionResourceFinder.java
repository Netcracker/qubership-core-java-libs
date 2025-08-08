package com.netcracker.cloud.dbaas.client.service.migration;

import org.qubership.cloud.dbaas.client.cassandra.migration.service.resource.SchemaVersionResourceFinder;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.qubership.cloud.dbaas.client.cassandra.migration.SchemaMigrationCommonConstants.DEFAULT_RESOURCE_SEPARATOR;

public class SpringBootJarSchemaVersionResourceFinder implements SchemaVersionResourceFinder {
    private final static String GET_ALL_SYMBOL = "*";

    private final PathMatchingResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();

    @Override
    public List<String> findResourceNames(URI resourceHolderUri, String resourceLocation) throws IOException {
        Resource[] resources = resourcePatternResolver.getResources(resourceLocation + DEFAULT_RESOURCE_SEPARATOR + GET_ALL_SYMBOL);

        List<String> resourcePaths = new ArrayList<>();
        for (Resource resource : resources) {
            resourcePaths.add(resource.getFilename());
        }

        return resourcePaths;
    }
}
