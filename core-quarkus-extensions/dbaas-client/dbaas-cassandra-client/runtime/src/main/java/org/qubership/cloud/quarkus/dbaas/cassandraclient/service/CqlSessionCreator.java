package org.qubership.cloud.quarkus.dbaas.cassandraclient.service;

import com.datastax.oss.driver.api.core.CqlSession;
import org.qubership.cloud.dbaas.client.cassandra.entity.connection.CassandraDBConnection;
import org.qubership.cloud.dbaas.client.cassandra.entity.database.CassandraDatabase;

public interface CqlSessionCreator {

    /**
     * @deprecated
     * This method doesn't support proper metric registration.
     * <p> Use {@link CqlSessionCreator#createSession(CassandraDatabase)} instead.
     */
    @Deprecated(forRemoval = true)
    CqlSession createSession(CassandraDBConnection connectionProperties);

    CqlSession createSession(CassandraDatabase cassandraDatabase);
}
