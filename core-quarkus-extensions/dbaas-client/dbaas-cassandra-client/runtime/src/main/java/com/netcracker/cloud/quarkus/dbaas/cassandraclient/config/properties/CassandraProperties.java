package com.netcracker.cloud.quarkus.dbaas.cassandraclient.config.properties;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;
import io.smallrye.config.WithParentName;

@ConfigMapping(prefix = "quarkus.dbaas.cassandra")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface CassandraProperties {

    /**
     * Cassandra DB Creation parameters.
     */
    @WithName("api")
    DbaaSCassandraDbCreationConfig cassandraDbCreationConfig();

    /**
     * Common properties.
     */
    @WithParentName
    CassandraSessionProperties cassandraSessionProperties();
}
