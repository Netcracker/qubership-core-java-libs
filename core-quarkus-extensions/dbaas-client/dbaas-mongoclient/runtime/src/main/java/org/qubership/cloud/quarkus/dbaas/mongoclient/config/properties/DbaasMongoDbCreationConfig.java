package org.qubership.cloud.quarkus.dbaas.mongoclient.config.properties;

import org.qubership.cloud.dbaas.common.config.DbaasApiPropertiesConfig;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "dbaas.mongo.api", phase = ConfigPhase.RUN_TIME)
public class DbaasMongoDbCreationConfig {
    /**
     * Property with MongoDB role which send request and database name prefix.
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public DbaasApiPropertiesConfig dbaasApiPropertiesConfig;

}
