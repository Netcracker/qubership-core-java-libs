package org.qubership.cloud.core.quarkus.dbaas.datasource.config.flyway;

import io.quarkus.runtime.util.StringUtil;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.jetbrains.annotations.Nullable;

@ApplicationScoped
@Priority(Integer.MAX_VALUE)
public class FlywayConfigurationCustomizerDefaultImpl implements FlywayConfigurationCustomizer {

    private static final String[] EMPTY_ARRAY = new String[0];
    private static final String DEFAULT_MIGRATION_LOCATION = "db/migration";

    @Inject
    CoreFlywayConfig coreFlywayConfig;

    @Override
    public void customize(FluentConfiguration configure) {
        customize(configure, null);
    }

    @Override
    public void customize(FluentConfiguration configure, @Nullable String logicalDbName) {
        FlywayConfig flywayConfig = coreFlywayConfig.globalFlywayConfig;
        if (!StringUtil.isNullOrEmpty(logicalDbName) && coreFlywayConfig.datasources.containsKey(logicalDbName)) {
            flywayConfig = coreFlywayConfig.datasources.get(logicalDbName);
        }
        String locations = flywayConfig.location.orElse(DEFAULT_MIGRATION_LOCATION);
        if (!StringUtil.isNullOrEmpty(logicalDbName)) {
            locations = flywayConfig.location.orElse("versioned/" + DEFAULT_MIGRATION_LOCATION + "/" + logicalDbName);
        }
        if (flywayConfig.connectRetries.isPresent()) {
            configure.connectRetries(flywayConfig.connectRetries.getAsInt());
        }
        flywayConfig.schemas.ifPresent(strings -> configure.schemas(strings.toArray(EMPTY_ARRAY)));
        flywayConfig.table.ifPresent(configure::table);
        configure.locations(locations.split(","));
        flywayConfig.sqlMigrationPrefix.ifPresent(configure::sqlMigrationPrefix);
        flywayConfig.repeatableSqlMigrationPrefix.ifPresent(configure::repeatableSqlMigrationPrefix);
        configure.baselineOnMigrate(flywayConfig.baselineOnMigrate);
        configure.validateOnMigrate(flywayConfig.validateOnMigrate);
        configure.baselineVersion(flywayConfig.baselineVersion);
        configure.outOfOrder(flywayConfig.outOfOrder);
        flywayConfig.ignoreMigrationPatterns.ifPresent(configure::ignoreMigrationPatterns);
        configure.cleanDisabled(flywayConfig.cleanDisabled);

        flywayConfig.baselineDescription.ifPresent(configure::baselineDescription);
        configure.createSchemas(flywayConfig.createSchemas);
    }
}
