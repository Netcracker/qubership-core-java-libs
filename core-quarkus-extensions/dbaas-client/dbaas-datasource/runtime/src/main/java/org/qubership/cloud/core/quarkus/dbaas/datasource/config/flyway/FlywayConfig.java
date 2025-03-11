package org.qubership.cloud.core.quarkus.dbaas.datasource.config.flyway;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

@ConfigGroup
public class FlywayConfig {

    /**
     * location
     */
    @ConfigItem
    public Optional<String> location;

    /**
     * connectRetries
     */
    @ConfigItem
    public OptionalInt connectRetries;

    /**
     * schemas
     */
    @ConfigItem
    public Optional<List<String>> schemas;

    /**
     * table
     */
    @ConfigItem
    public Optional<String> table;

    /**
     * sqlMigrationPrefix
     */
    @ConfigItem
    public Optional<String> sqlMigrationPrefix;

    /**
     * repeatableSqlMigrationPrefix
     */
    @ConfigItem
    public Optional<String> repeatableSqlMigrationPrefix;

    /**
     * clean and run migration at start time
     */
    @ConfigItem(defaultValue = "false")
    public boolean cleanAndMigrateAtStart;

    /**
     * baselineOnMigrate
     */
    @ConfigItem(defaultValue = "true")
    public boolean baselineOnMigrate;

    /**
     * The initial baseline version.
     */
    @ConfigItem(defaultValue = "1")
    public String baselineVersion;

    /**
     * baselineDescription.
     */
    @ConfigItem
    public Optional<String> baselineDescription;

    /**
     * validateOnMigrate
     */
    @ConfigItem(defaultValue = "true")
    public boolean validateOnMigrate;

    /**
     * createSchemas
     */
    @ConfigItem(defaultValue = "true")
    public boolean createSchemas;

    /**
     * outOfOrder
     */
    @ConfigItem(defaultValue = "false")
    public boolean outOfOrder;

    /**
     * ignoreMissingMigrations
     */
    @ConfigItem
    public Optional<String[]> ignoreMigrationPatterns;

    /**
     * cleanDisabled
     */
    @ConfigItem(defaultValue = "false")
    public boolean cleanDisabled;

}
