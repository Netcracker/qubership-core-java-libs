package org.qubership.cloud.core.quarkus.dbaas.datasource.config.properties;

import org.qubership.cloud.dbaas.client.entity.settings.PostgresSettings;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

import java.util.Optional;

@ConfigGroup
public class PostgresDbConfiguration {
    /**
     * Property with PostgresSettings. Contains:
     * - pgExtensions. List of possible extensions to add to postgres database.
     */
    @ConfigItem(name = "database-settings")
    public Optional<PostgresSettings> databaseSettings;

    /**
     * Property with physical database id.
     */
    @ConfigItem(name = "physical-database-id")
    public Optional<String> physicalDatabaseId;
}
