package com.netcracker.cloud.quarkus.dbaas.cassandraclient.config.properties;

import com.netcracker.cloud.dbaas.common.config.DbaasApiPropertiesConfig;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import io.smallrye.config.WithParentName;

import java.util.Map;

public interface DbaaSCassandraDbCreationConfig {

    /**
     * Property with Cassandra creation parameters for service database.
     */
    @WithName("service")
    CassandraDbConfiguration serviceDbConfiguration();

    /**
     * Property with Cassandra creation parameters for tenant databases.
     */
    @WithName("tenant")
    Map<String, CassandraDbConfiguration> tenantDbConfiguration();

    /**
     * Property with DB Classifier.
     */
    @WithName("db-classifier")
    @WithDefault("default")
    String dbClassifier();

    /**
     * Property with database name prefix and runtime user role.
     */
    @WithParentName
    DbaasApiPropertiesConfig dbaasApiPropertiesConfig();

    default CassandraDbConfiguration getCassandraDbConfiguration(String tenantId) {
        if (tenantId != null) {
            Map<String, CassandraDbConfiguration> tenants = tenantDbConfiguration();
            if (tenants != null) {
                CassandraDbConfiguration cassandraDbConfiguration = tenants.get(tenantId);
                return cassandraDbConfiguration != null ? cassandraDbConfiguration : tenants.get("tenant");
            }
        }
        return serviceDbConfiguration();
    }
}
