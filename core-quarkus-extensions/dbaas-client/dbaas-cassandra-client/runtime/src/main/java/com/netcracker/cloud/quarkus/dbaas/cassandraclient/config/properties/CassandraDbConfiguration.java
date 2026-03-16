package com.netcracker.cloud.quarkus.dbaas.cassandraclient.config.properties;

import io.smallrye.config.WithName;

import java.util.Optional;

public interface CassandraDbConfiguration {

    /**
     * Property with physical database id.
     */
    @WithName("physical-database-id")
    Optional<String> physicalDatabaseId();

    /**
     * Property with database settings.
     */
    @WithName("database-settings")
    CassandraDatabaseSettings databaseSettings();
}
