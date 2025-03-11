package org.qubership.cloud.core.quarkus.dbaas.datasource.service;

import javax.sql.DataSource;

public interface MigrationService {
    int migrate(DataSource dataSource);

    default int migrate(DataSource dataSource, String logicalDbName) {
        return migrate(dataSource);
    }
}
