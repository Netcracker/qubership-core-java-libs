package org.qubership.cloud.dbaas.client.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.qubership.cloud.dbaas.client.management.DatabaseConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

class CassandraDatabaseSettingsTest {

    @Test
    public void testSerialiseSettings() throws JsonProcessingException {
        var settings = Map.of("replication", (Object) "{'class': 'SimpleStrategy','replication_factor': 1}");
        var classifier = Map.of("microserviceName", (Object) "test-ms");
        final CassandraDatabaseSettings cassandraDatabaseSettings = new CassandraDatabaseSettings();
        cassandraDatabaseSettings.setSettings(settings);
        var expectedSettings = ",\"settings\":{\"replication\":\"{'class': 'SimpleStrategy','replication_factor': 1}\"}";

        final DatabaseConfig databaseConfig = DatabaseConfig.builder().databaseSettings(cassandraDatabaseSettings).build();
        final DatabaseCreateRequest databaseCreateRequest = new DatabaseCreateRequest(classifier, "cassandra", databaseConfig);
        String databaseCreateRequestJson = new ObjectMapper().writeValueAsString(databaseCreateRequest);
        Assertions.assertTrue(databaseCreateRequestJson.contains(expectedSettings));
    }
}