package com.netcracker.cloud.quarkus.dbaas.mongoclient.config.properties;

import com.netcracker.cloud.dbaas.common.config.DbaasApiPropertiesConfig;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithParentName;

@ConfigMapping(prefix = "quarkus.dbaas.mongo.api")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface DbaasMongoDbCreationConfig {

    /**
     * Property with MongoDB role which send request and database name prefix.
     */
    @WithParentName
    DbaasApiPropertiesConfig dbaasApiPropertiesConfig();
}
