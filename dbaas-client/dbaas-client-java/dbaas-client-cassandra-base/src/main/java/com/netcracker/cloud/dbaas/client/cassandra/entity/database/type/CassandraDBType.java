package org.qubership.cloud.dbaas.client.cassandra.entity.database.type;

import org.qubership.cloud.dbaas.client.cassandra.entity.connection.CassandraDBConnection;
import org.qubership.cloud.dbaas.client.cassandra.entity.database.CassandraDatabase;
import org.qubership.cloud.dbaas.client.entity.database.type.DatabaseType;

import static org.qubership.cloud.dbaas.client.entity.database.type.PhysicalDbType.CASSANDRA;

/**
 * The class used to invoke the API of {@link org.qubership.cloud.dbaas.client.DbaasClient}
 * which can operate with cassandra database
 * <p>
 * usage example:
 *
 * <pre>{@code
 *      CassandraDatabase cassandraDatabase = dbaasClient.createDatabase(CassandraDBType.INSTANCE, namespace, classifier);
 *      CassandraDBConnection cassandraDBConnection = dbaasClient.getConnection(CassandraDBType.INSTANCE, namespace, classifier);
 *      dbaasClient.deleteDatabase(cassandraDatabase);
 *  }</pre>
 */
public class CassandraDBType extends DatabaseType<CassandraDBConnection, CassandraDatabase> {

    public static final CassandraDBType INSTANCE = new CassandraDBType();

    private CassandraDBType() {
        super(CASSANDRA, CassandraDatabase.class);
    }
}
