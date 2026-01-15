package com.netcracker.cloud.dbaas.client.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netcracker.cloud.dbaas.client.entity.database.DatabaseSettings;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
public class MongoDatabaseSettings implements DatabaseSettings {
    public static final String SHARDING_PROPERTY_NAME = "sharding-settings";
    public static final String TARGET_SHARD_PROPERTY_NAME = "target-shard";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonNode shardingSettings;
    private String targetShard;

    public MongoDatabaseSettings(Map<String, Object> databaseSettings) {
        if (databaseSettings == null || databaseSettings.isEmpty()) {
            return;
        }

        if (databaseSettings.containsKey(SHARDING_PROPERTY_NAME)) {
            try {
                shardingSettings = MAPPER.readTree(databaseSettings.get(SHARDING_PROPERTY_NAME).toString());
            } catch (JsonProcessingException e) {
                throw new RuntimeException(String.format("'%s' property value should be a valid JSON array, but got: %s",
                        SHARDING_PROPERTY_NAME, databaseSettings.get(SHARDING_PROPERTY_NAME)));
            }
        }

        if (databaseSettings.containsKey(TARGET_SHARD_PROPERTY_NAME)) {
            targetShard = databaseSettings.get(TARGET_SHARD_PROPERTY_NAME).toString();
        }
    }
}
