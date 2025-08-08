package com.netcracker.cloud.dbaas.client.entity;

import java.util.Map;
import com.fasterxml.jackson.annotation.JsonValue;
import org.qubership.cloud.dbaas.client.entity.database.DatabaseSettings;
import lombok.Data;

@Data
public class CassandraDatabaseSettings implements DatabaseSettings {
    @JsonValue
    Map<String,Object> settings;
}
