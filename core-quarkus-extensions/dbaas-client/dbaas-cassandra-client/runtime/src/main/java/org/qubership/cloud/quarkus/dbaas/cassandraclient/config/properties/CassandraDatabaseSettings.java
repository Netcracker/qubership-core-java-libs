package org.qubership.cloud.quarkus.dbaas.cassandraclient.config.properties;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import lombok.NoArgsConstructor;
import org.qubership.cloud.dbaas.client.entity.database.DatabaseSettings;

import java.util.Optional;

@ConfigGroup
@NoArgsConstructor
public class CassandraDatabaseSettings {
    /**
     * Property with replication setting.
     */
    @ConfigItem(name = "replication")
    public Optional<String> replication;

    public boolean isEmpty() {
        return replication.isEmpty();
    }

    public CassandraSettingsDTO toDTO() {
        return new CassandraSettingsDTO(replication.orElse(null));
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record CassandraSettingsDTO(String replication) implements DatabaseSettings {
    }
}
