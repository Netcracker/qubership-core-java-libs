package org.qubership.cloud.dbaas.client.entity;

import lombok.Data;

import java.util.Map;

@Data
public class DbaasApiProperties {
    private String runtimeUserRole;
    private String dbPrefix;
    private int retryAttempts;
    private long retryDelay;

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
