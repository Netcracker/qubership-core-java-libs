package com.netcracker.cloud.core.quarkus.dbaas.datasource.config.properties;

import com.netcracker.cloud.dbaas.client.entity.settings.PostgresSettings;
import com.netcracker.cloud.dbaas.common.config.DbaasApiPropertiesConfig;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;
import io.smallrye.config.WithParentName;

import java.util.Map;
import java.util.Optional;

@ConfigMapping(prefix = "quarkus.dbaas.postgresql.api")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface DbaaSPostgresDbCreationConfig {
    /**
     * Property with postgreSQL creation parameters for service database.
     */
    @WithName("service.database-settings")
    Optional<PostgresSettings> serviceDatabaseSettings();

    /**
     * Property with postgreSQL creation parameters for tenant databases.
     */
    @WithName("service.physical-database-id")
    Optional<String> servicePhysicalDatabaseId();

    /**
     * Property with postgreSQL creation parameters for all tenants databases.
     */
    @WithName("tenant")
    Map<String, TenantConfig> tenantDbConfiguration();

    /**
     * Property with DatabaseSettings parameter for all tenants databases.
     */
    @WithName("tenant.database-settings")
    Optional<PostgresSettings> allTenantsDatabaseSettings();

    /**
     * Property with PhysicalDatabaseId parameter for all tenants databases.
     */
    @WithName("tenant.physical-database-id")
    Optional<String> allTenantsPhysicalDatabaseId();

    /**
     * Property with postgreSQL role which send request and database name prefix.
     */
    @WithParentName
    DbaasApiPropertiesConfig dbaasApiPropertiesConfig();

    default PostgresDbConfiguration getPostgresDbConfiguration(String tenantId) {
        if (tenantId == null) {
            return new PostgresDbConfiguration(
                    serviceDatabaseSettings(),
                    servicePhysicalDatabaseId()
            );
        }

        Map<String, TenantConfig> tenants = tenantDbConfiguration();
        if (tenants != null) {
            TenantConfig specific = tenants.get(tenantId);
            if (specific != null) {
                return new PostgresDbConfiguration(
                        specific.databaseSettings(),
                        specific.physicalDatabaseId()
                );
            }
        }

        // fallback: get for all tenants
        if (allTenantsDatabaseSettings().isPresent() || allTenantsPhysicalDatabaseId().isPresent()) {
            return new PostgresDbConfiguration(
                    allTenantsDatabaseSettings(),
                    allTenantsPhysicalDatabaseId()
            );
        }

        // final fallback: get for service
        return new PostgresDbConfiguration(
                serviceDatabaseSettings(),
                servicePhysicalDatabaseId()
        );
    }

    interface TenantConfig {

        /**
         * dbaas.postgresql.api.tenant.<tenant-id>.database-settings
         */
        @WithName("database-settings")
        Optional<PostgresSettings> databaseSettings();

        /**
         * dbaas.postgresql.api.tenant.<tenant-id>.physical-database-id
         */
        @WithName("physical-database-id")
        Optional<String> physicalDatabaseId();
    }
}
