package org.qubership.cloud.dbaas.client;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class FlywayConfigurationProperties {
    private Map<String, Datasource> datasources = new HashMap<>();
    private Datasource datasource;

    @Data
    public static class Datasource {
        private Map<String, String> flyway = new HashMap<>();
    }
}
