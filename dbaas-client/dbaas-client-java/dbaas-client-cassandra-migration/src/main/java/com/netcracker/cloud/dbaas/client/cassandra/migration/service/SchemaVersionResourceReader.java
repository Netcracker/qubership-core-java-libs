package com.netcracker.cloud.dbaas.client.cassandra.migration.service;

import com.netcracker.cloud.dbaas.client.cassandra.migration.model.SchemaVersionFromResource;

import java.util.List;

/**
 * Provides the ability to read resources holding schema versions.
 */
public interface SchemaVersionResourceReader {
    /**
     * @return schema versions list read from resources
     */
    List<SchemaVersionFromResource> readSchemaVersionResources();
}
