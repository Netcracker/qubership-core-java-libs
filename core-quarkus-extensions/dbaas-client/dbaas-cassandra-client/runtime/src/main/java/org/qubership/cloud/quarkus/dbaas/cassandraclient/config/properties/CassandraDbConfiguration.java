package org.qubership.cloud.quarkus.dbaas.cassandraclient.config.properties;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import lombok.Getter;

import java.util.Optional;

@Getter
@ConfigGroup
public class CassandraDbConfiguration {

    /**
     * Property with physical database id.
     */
    @ConfigItem(name = "physical-database-id")
    Optional<String> physicalDatabaseId;
}
