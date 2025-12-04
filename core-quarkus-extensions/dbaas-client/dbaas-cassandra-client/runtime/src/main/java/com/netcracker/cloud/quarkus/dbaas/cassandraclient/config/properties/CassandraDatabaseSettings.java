package com.netcracker.cloud.quarkus.dbaas.cassandraclient.config.properties;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.netcracker.cloud.dbaas.client.entity.database.DatabaseSettings;
import io.smallrye.config.WithName;

import java.util.Optional;

public interface CassandraDatabaseSettings {
    /**
     * Property with replication setting.
     */
    @WithName("replication")
    Optional<String> replication();

    default boolean isEmpty() {
        return replication().isEmpty();
    }

    default CassandraSettingsDTO toDTO() {
        return new CassandraSettingsDTO(replication().orElse(null));
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record CassandraSettingsDTO(String replication) implements DatabaseSettings {
    }
}
