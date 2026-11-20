package com.netcracker.cloud.dbaas.client.management;

import com.netcracker.cloud.dbaas.client.entity.database.DatabaseSettings;
import lombok.Data;

@Data
@lombok.Builder(toBuilder = true, builderClassName = "Builder")
public class DatabaseConfig {
    private String dbNamePrefix;
    private Boolean backupDisabled;
    private DatabaseSettings databaseSettings;
    private String physicalDatabaseId;
    private String userRole;
}
