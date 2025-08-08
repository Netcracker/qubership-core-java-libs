package com.netcracker.cloud.dbaas.client.entity.database;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class DbaasClickhouseDatasourceProperties {
    private Map<String, String> datasourceProperties = new HashMap<>();
}
