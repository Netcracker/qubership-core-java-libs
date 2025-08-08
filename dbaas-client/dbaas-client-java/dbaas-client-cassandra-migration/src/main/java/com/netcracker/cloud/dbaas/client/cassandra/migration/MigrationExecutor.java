package org.qubership.cloud.dbaas.client.cassandra.migration;

import com.datastax.oss.driver.api.core.CqlSession;

public interface MigrationExecutor {
    void migrate(CqlSession session);
}
