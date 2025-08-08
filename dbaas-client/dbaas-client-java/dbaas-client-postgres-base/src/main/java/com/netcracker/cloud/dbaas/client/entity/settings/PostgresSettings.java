package org.qubership.cloud.dbaas.client.entity.settings;

import org.qubership.cloud.dbaas.client.entity.database.DatabaseSettings;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class PostgresSettings implements DatabaseSettings {
    static final String PG_EXTENSIONS = "pg-extensions";

    List<String> pgExtensions;

    public PostgresSettings(Map<String, Object> databataseSettings) {
        if (databataseSettings!=null && databataseSettings.containsKey(PG_EXTENSIONS)) {
            pgExtensions = Arrays.stream(databataseSettings.get(PG_EXTENSIONS).toString().split(",")).toList();
        }
    }
}
