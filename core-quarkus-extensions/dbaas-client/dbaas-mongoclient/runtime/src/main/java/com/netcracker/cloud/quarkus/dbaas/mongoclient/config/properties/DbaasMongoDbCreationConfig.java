package com.netcracker.cloud.quarkus.dbaas.mongoclient.config.properties;

import com.netcracker.cloud.dbaas.client.entity.MongoDatabaseSettings;
import com.netcracker.cloud.dbaas.common.config.DbaasApiPropertiesConfig;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;
import io.smallrye.config.WithParentName;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@ConfigMapping(prefix = "quarkus.dbaas.mongo.api")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface DbaasMongoDbCreationConfig {

    /**
     * Property with MongoDB creation parameters for service database.
     */
    @WithName("service.database-settings")
    Optional<InternalMongoDatabaseSettings> serviceDatabaseSettings();

    /**
     * Property with MongoDB creation parameters for all tenants databases.
     */
    @WithName("tenant")
    Map<String, TenantConfig> tenantDbConfiguration();

    /**
     * Property with DatabaseSettings parameter for all tenants databases.
     */
    @WithName("tenant.database-settings")
    Optional<InternalMongoDatabaseSettings> allTenantsDatabaseSettings();

    /**
     * Property with MongoDB role which send request and database name prefix.
     */
    @WithParentName
    DbaasApiPropertiesConfig dbaasApiPropertiesConfig();

    default MongoDbConfiguration getMongoDbConfiguration(String tenantId) {
        if (tenantId == null) {
            return new MongoDbConfiguration(serviceDatabaseSettings());
        }

        Map<String, TenantConfig> tenants = tenantDbConfiguration();
        if (tenants != null) {
            TenantConfig specific = tenants.get(tenantId);
            if (specific != null) {
                return new MongoDbConfiguration(specific.databaseSettings());
            }
        }

        // fallback: get for all tenants
        if (allTenantsDatabaseSettings().isPresent()) {
            return new MongoDbConfiguration(allTenantsDatabaseSettings());
        }

        // final fallback: get for service
        return new MongoDbConfiguration(serviceDatabaseSettings());
    }

    interface TenantConfig {
        /**
         * dbaas.mongo.api.tenant.<tenant-id>.database-settings
         */
        @WithName("database-settings")
        Optional<InternalMongoDatabaseSettings> databaseSettings();
    }

    interface InternalMongoDatabaseSettings {
        /**
         * Property with MongoDB creation parameters for service database.
         */
        @WithName("sharding-settings")
        Optional<String> shardingSettings();

        /**
         * Property with MongoDB creation parameters for service database.
         */
        @WithName("target-shard")
        Optional<String> targetShard();

        default MongoDatabaseSettings toMongoSettings() {
            Map<String, Object> settings = new HashMap<>();
            shardingSettings().ifPresent(s -> settings.put("sharding-settings", s));
            targetShard().ifPresent(s -> settings.put("target-shard", s));
            return new MongoDatabaseSettings(settings);
        }
    }
}
