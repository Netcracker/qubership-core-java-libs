package com.netcracker.cloud.dbaas.client.entity.database.type;

import org.qubership.cloud.dbaas.client.entity.connection.MongoDBConnection;
import org.qubership.cloud.dbaas.client.entity.database.MongoDatabase;

import static org.qubership.cloud.dbaas.client.entity.database.type.PhysicalDbType.MONGODB;

/**
 * The class used to invoke the API of {@link org.qubership.cloud.dbaas.client.DbaasClient}
 * which can operate with mongodb database
 * <p>
 * usage example:
 *
 * <pre>{@code
 *      MongoDatabase mongoDatabase = dbaasClient.createDatabase(MongoDBType.INSTANCE, namespace, classifier);
 *      MongoDBConnection mongoDBConnection = dbaasClient.getConnection(MongoDBType.INSTANCE, namespace, classifier);
 *      dbaasClient.deleteDatabase(mongoDatabase);
 *  }</pre>
 */
public class MongoDBType extends DatabaseType<MongoDBConnection, MongoDatabase> {

    public static final MongoDBType INSTANCE = new MongoDBType();

    private MongoDBType() {
        super(MONGODB, MongoDatabase.class);
    }
}
