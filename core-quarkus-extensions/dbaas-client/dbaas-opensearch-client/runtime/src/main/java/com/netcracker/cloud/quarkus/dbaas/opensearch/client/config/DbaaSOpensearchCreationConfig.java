package com.netcracker.cloud.quarkus.dbaas.opensearch.client.config;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

import java.util.Map;
import java.util.Optional;

@ConfigMapping(prefix = "quarkus.dbaas.opensearch.api")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface DbaaSOpensearchCreationConfig {

    /**
     * physical-database-id with opensearch creation parameters for service database.
     */
    @WithName("service.physical-database-id")
    Optional<String> servicePhysicalDatabaseId();

    /**
     * prefix-config with opensearch creation parameters for service database.
     */
    @WithName("service.prefix-config")
    SinglePrefix servicePrefixConfig();

    /**
     * Property with opensearch creation parameters for tenant databases.
     */
    @WithName("tenant")
    Map<String, TenantConfig> tenantDbConfiguration();

    /**
     * physical-database-id for all tenant database.
     */
    @WithName("tenant.physical-database-id")
    Optional<String> singleTenantPhysicalDatabaseId();

    /**
     * prefix-config with for all tenant database.
     */
    @WithName("tenant.prefix-config")
    SinglePrefix singleTenantPrefixConfig();

    /**
     * Property with user role for outgoing requests.
     */
    @WithName("runtime-user-role")
    Optional<String> runtimeUserRole();

    default OpensearchConfiguration getOpensearchConfiguration(String tenantId) {
        if (tenantId == null) {
            return new OpensearchConfiguration(
                    servicePhysicalDatabaseId(),
                    servicePrefixConfig()
            );
        }

        Map<String, TenantConfig> tenants = tenantDbConfiguration();
        if (tenants != null) {
            TenantConfig specific = tenants.get(tenantId);
            if (specific != null) {
                return new OpensearchConfiguration(
                        specific.physicalDatabaseId(),
                        specific.prefixConfig()
                );
            }
        }

        // fallback: get for all tenants
        return new OpensearchConfiguration(
                singleTenantPhysicalDatabaseId(),
                singleTenantPrefixConfig()
        );
    }

    interface TenantConfig {

        /**
         * physical-database-id with opensearch creation parameters for tenant database.
         */
        @WithName("physical-database-id")
        Optional<String> physicalDatabaseId();

        /**
         * prefix-config with opensearch creation parameters for tenant database.
         */
        @WithName("prefix-config")
        SinglePrefix prefixConfig();
    }
}
