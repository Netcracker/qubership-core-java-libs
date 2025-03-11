package org.qubership.cloud.core.quarkus.dbaas.datasource.config.flyway;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

import java.util.Map;

@ConfigRoot(name = "dbaas.flyway", phase = ConfigPhase.RUN_TIME)
public class CoreFlywayConfig {
    /**
     * flywayConfig
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public FlywayConfig globalFlywayConfig;

    /**
     * datasources
     */
    @ConfigMapping(prefix = "datasources")
    public Map<String, FlywayConfig> datasources;

}
