package com.netcracker.cloud.dbaas.client.entity;

import org.qubership.cloud.dbaas.client.entity.database.DatabaseSettings;
import org.qubership.cloud.dbaas.client.management.DatabaseConfig;
import lombok.Data;
import lombok.NonNull;

import java.util.Map;

@Data
public class DatabaseCreateRequest {
    @NonNull
    private Map<String, Object> classifier;
    @NonNull
    private String type;
    private String namePrefix;
    private String physicalDatabaseId;
    private Boolean backupDisabled;
    private DatabaseSettings settings;
    private String userRole;

    public DatabaseCreateRequest() {
    }

    public DatabaseCreateRequest(@NonNull Map<String, Object> classifier, @NonNull String type, DatabaseConfig databaseConfig) {
        this.classifier = classifier;
        this.type = type;
        this.namePrefix = databaseConfig.getDbNamePrefix();
        this.physicalDatabaseId = databaseConfig.getPhysicalDatabaseId();
        this.backupDisabled = databaseConfig.getBackupDisabled();
        this.settings = databaseConfig.getDatabaseSettings();
        this.userRole = databaseConfig.getUserRole();
    }
}
