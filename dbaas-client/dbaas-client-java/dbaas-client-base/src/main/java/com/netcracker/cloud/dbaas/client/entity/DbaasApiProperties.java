package com.netcracker.cloud.dbaas.client.entity;

import lombok.Data;

import java.util.Map;

@Data
public class DbaasApiProperties {
    public static final int DEFAULT_RETRIES = 5;
    public static final long DEFAULT_RETRY_DELAY_MS = 5_000L;

    private String runtimeUserRole;
    private String dbPrefix;
    private int retryAttempts = DEFAULT_RETRIES;
    private long retryDelay = DEFAULT_RETRY_DELAY_MS;


    private Map<String, Object> databaseSettings;
    private DbScopeProperties service = new DbScopeProperties();
    private DbScopeProperties tenant = new DbScopeProperties();

    public enum DbScope {
        SERVICE, TENANT
    }

    public Map<String, Object> getDatabaseSettings(DbScope scope) {
        return scope.equals(DbScope.SERVICE) ? service.databaseSettings :
                scope.equals(DbScope.TENANT) ? tenant.databaseSettings : databaseSettings;
    }
    @Data
    public static class DbScopeProperties {
        public Map<String, Object> databaseSettings;
    }
}
