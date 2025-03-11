package org.qubership.cloud.quarkus.dbaas.cassandraclient.config.properties;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import lombok.Getter;


@Getter
@ConfigRoot(name = "dbaas.cassandra", phase = ConfigPhase.RUN_TIME)
public class CassandraProperties {

    /**
     * Cassandra DB Creation parameters.
     */
    @ConfigItem(name = "api")
    DbaaSCassandraDbCreationConfig cassandraDbCreationConfig;

    /**
     * Common properties.
     */
    @ConfigItem(name = ConfigItem.PARENT)
    CassandraSessionProperties cassandraSessionProperties;
}

