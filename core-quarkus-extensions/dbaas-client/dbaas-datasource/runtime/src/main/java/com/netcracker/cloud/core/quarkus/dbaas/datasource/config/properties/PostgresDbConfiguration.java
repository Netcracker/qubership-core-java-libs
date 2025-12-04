package com.netcracker.cloud.core.quarkus.dbaas.datasource.config.properties;

import com.netcracker.cloud.dbaas.client.entity.settings.PostgresSettings;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Optional;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PostgresDbConfiguration {

    /**
     * Property with PostgresSettings. Contains:
     * - pgExtensions. List of possible extensions to add to postgres database.
     */
    private Optional<PostgresSettings> databaseSettings;

    /**
     * Property with physical database id.
     */
    private Optional<String> physicalDatabaseId;
}
