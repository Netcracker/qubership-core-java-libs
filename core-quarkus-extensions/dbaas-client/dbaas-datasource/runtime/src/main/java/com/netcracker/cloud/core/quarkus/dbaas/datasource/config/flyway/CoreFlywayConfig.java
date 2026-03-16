package com.netcracker.cloud.core.quarkus.dbaas.datasource.config.flyway;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;
import io.smallrye.config.WithParentName;

import java.util.Map;

@ConfigMapping(prefix = "quarkus.dbaas.flyway")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface CoreFlywayConfig {

    /**
     * flywayConfig
     */
    @WithParentName
    FlywayConfig globalFlywayConfig();

    /**
     * datasources
     */
    @WithName("datasources")
    Map<String, FlywayConfig> datasources();
}
