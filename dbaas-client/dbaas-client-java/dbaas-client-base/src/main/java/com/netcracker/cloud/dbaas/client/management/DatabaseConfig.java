package com.netcracker.cloud.dbaas.client.management;

import org.qubership.cloud.dbaas.client.entity.database.DatabaseSettings;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true, builderClassName = "Builder")
public class DatabaseConfig {
    private String dbNamePrefix;
    private Boolean backupDisabled;
    private DatabaseSettings databaseSettings;
    private String physicalDatabaseId;
    private String userRole;
}
