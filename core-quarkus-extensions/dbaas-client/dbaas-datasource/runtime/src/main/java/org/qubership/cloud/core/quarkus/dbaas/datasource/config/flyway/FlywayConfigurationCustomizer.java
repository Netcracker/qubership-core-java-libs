package org.qubership.cloud.core.quarkus.dbaas.datasource.config.flyway;

import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.jetbrains.annotations.Nullable;

public interface FlywayConfigurationCustomizer {
    void customize(FluentConfiguration fluentConfiguration);

    default void customize(FluentConfiguration fluentConfiguration, @Nullable String logicalDbName) {
        customize(fluentConfiguration);
    }
}
