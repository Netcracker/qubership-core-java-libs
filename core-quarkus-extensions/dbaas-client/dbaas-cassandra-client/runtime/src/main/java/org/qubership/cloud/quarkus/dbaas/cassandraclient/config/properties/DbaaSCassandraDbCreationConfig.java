package org.qubership.cloud.quarkus.dbaas.cassandraclient.config.properties;

import org.qubership.cloud.dbaas.common.config.DbaasApiPropertiesConfig;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import lombok.Getter;

import java.util.Map;

@Getter
@ConfigGroup
public class DbaaSCassandraDbCreationConfig {

    /**
     * Property with Cassandra creation parameters for service database.
     */
    @ConfigItem(name = "service")
    CassandraDbConfiguration serviceDbConfiguration;

    /**
     * Property with Cassandra creation parameters for tenant databases.
     */
    @ConfigItem(name = "tenant")
    Map<String, CassandraDbConfiguration> tenantDbConfiguration;

    /**
     * Property with DB Classifier.
     */
    @ConfigItem(name = "db-classifier", defaultValue = "default")
    String dbClassifier;

    /**
     * Property with database name prefix and runtime user role.
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public DbaasApiPropertiesConfig dbaasApiPropertiesConfig;

    public CassandraDbConfiguration getCassandraDbConfiguration(String tenantId) {
        if (tenantId != null) {
            CassandraDbConfiguration cassandraDbConfiguration = tenantDbConfiguration.get(tenantId);
            return cassandraDbConfiguration != null ? cassandraDbConfiguration : tenantDbConfiguration.get("tenant");
        } else {
            return serviceDbConfiguration;
        }
    }

}
