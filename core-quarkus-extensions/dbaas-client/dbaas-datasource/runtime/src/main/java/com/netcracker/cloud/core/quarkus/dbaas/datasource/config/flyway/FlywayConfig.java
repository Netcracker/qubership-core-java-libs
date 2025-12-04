package com.netcracker.cloud.core.quarkus.dbaas.datasource.config.flyway;

import io.smallrye.config.WithDefault;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

public interface FlywayConfig {

    /**
     * location
     */
    Optional<String> location();

    /**
     * connectRetries
     */
    OptionalInt connectRetries();

    /**
     * schemas
     */
    Optional<List<String>> schemas();

    /**
     * table
     */
    Optional<String> table();

    /**
     * sqlMigrationPrefix
     */
    Optional<String> sqlMigrationPrefix();

    /**
     * repeatableSqlMigrationPrefix
     */
    Optional<String> repeatableSqlMigrationPrefix();

    /**
     * clean and run migration at start time
     */
    @WithDefault("false")
    boolean cleanAndMigrateAtStart();

    /**
     * baselineOnMigrate
     */
    @WithDefault("true")
    boolean baselineOnMigrate();

    /**
     * The initial baseline version.
     */
    @WithDefault("1")
    String baselineVersion();

    /**
     * baselineDescription.
     */
    Optional<String> baselineDescription();

    /**
     * validateOnMigrate
     */
    @WithDefault("true")
    boolean validateOnMigrate();

    /**
     * createSchemas
     */
    @WithDefault("true")
    boolean createSchemas();

    /**
     * outOfOrder
     */
    @WithDefault("false")
    boolean outOfOrder();

    /**
     * ignoreMissingMigrations
     */
    Optional<String[]> ignoreMigrationPatterns();

    /**
     * cleanDisabled
     */
    @WithDefault("false")
    boolean cleanDisabled();
}
