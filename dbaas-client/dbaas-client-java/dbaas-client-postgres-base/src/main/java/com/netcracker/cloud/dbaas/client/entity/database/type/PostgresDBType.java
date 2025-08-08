package com.netcracker.cloud.dbaas.client.entity.database.type;

import com.netcracker.cloud.dbaas.client.entity.connection.PostgresDBConnection;
import com.netcracker.cloud.dbaas.client.entity.database.PostgresDatabase;
import com.netcracker.cloud.dbaas.client.DbaasClient;

/**
 * The class used to invoke the API of {@link DbaasClient}
 * which can operate with postgresql database
 *
 * usage example:
 *
 * <pre>{@code
 *      PostgresDatabase postgresDatabase = dbaasClient.createDatabase(PostgresDBType.INSTANCE, namespace, classifier);
 *      PostgresDBConnection postgresDBConnection = dbaasClient.getConnection(PostgresDBType.INSTANCE, namespace, classifier);
 *      dbaasClient.deleteDatabase(postgresDatabase);
 * }</pre>
 */
public class PostgresDBType extends DatabaseType<PostgresDBConnection, PostgresDatabase> {
    public static final PostgresDBType INSTANCE = new PostgresDBType();

    private PostgresDBType() {
        super(PhysicalDbType.POSTGRESQL, PostgresDatabase.class);
    }
}
