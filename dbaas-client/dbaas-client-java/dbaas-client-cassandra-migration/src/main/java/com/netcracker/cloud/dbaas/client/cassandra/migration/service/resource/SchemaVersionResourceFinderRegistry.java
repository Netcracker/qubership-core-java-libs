package com.netcracker.cloud.dbaas.client.cassandra.migration.service.resource;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Resource finder registry implementation.
 * Provides the ability to find resources by jar and file schemes by default.
 */
public class SchemaVersionResourceFinderRegistry {
    public static final String JAR_SCHEME = "jar";
    public static final String FILE_SCHEME = "file";

    private final ConcurrentMap<String, SchemaVersionResourceFinder> resourceFinders;

    public SchemaVersionResourceFinderRegistry() {
        this.resourceFinders = new ConcurrentHashMap<>();
        this.resourceFinders.put(JAR_SCHEME, new JarSchemaVersionResourceFinder());
        this.resourceFinders.put(FILE_SCHEME, new FileSystemSchemaVersionResourceFinder());
    }

    public void register(String scheme, SchemaVersionResourceFinder resourceFinder) {
        this.resourceFinders.put(scheme, resourceFinder);
    }

    public SchemaVersionResourceFinder getFinder(String scheme) {
        return resourceFinders.get(scheme.toLowerCase());
    }
}
