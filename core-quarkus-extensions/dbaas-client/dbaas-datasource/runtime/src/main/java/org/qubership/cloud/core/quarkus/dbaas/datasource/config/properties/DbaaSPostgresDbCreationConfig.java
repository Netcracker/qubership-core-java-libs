package org.qubership.cloud.core.quarkus.dbaas.datasource.config.properties;

import org.qubership.cloud.dbaas.common.config.DbaasApiPropertiesConfig;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

import java.util.Map;
import java.util.Optional;

@ConfigRoot(name = "dbaas.postgresql.api", phase = ConfigPhase.RUN_TIME)
public class DbaaSPostgresDbCreationConfig {
    /**
     * Property with postgreSQL creation parameters for service database.
     */
    @ConfigItem(name = "service")
    public PostgresDbConfiguration serviceDbConfiguration;

    /**
     * Property with postgreSQL creation parameters for tenant databases.
     */
    @ConfigItem(name = "tenant")
    public Map<String, PostgresDbConfiguration> tenantDbConfiguration;

    /**
     * Property with postgreSQL creation parameters for all tenants databases.
     */
    @ConfigItem(name = "tenant")
    public PostgresDbConfiguration allTenantsDbConfiguration;

    /**
     * Property with postgreSQL role which send request and database name prefix.
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public DbaasApiPropertiesConfig dbaasApiPropertiesConfig;

    public PostgresDbConfiguration getPostgresDbConfiguration(String tenantId) {
        if (tenantId == null)
            return serviceDbConfiguration;

        return Optional.ofNullable(tenantDbConfiguration)
                .flatMap(x -> Optional.ofNullable(tenantDbConfiguration.get(tenantId)))
                .orElse(allTenantsDbConfiguration);
    }
}
