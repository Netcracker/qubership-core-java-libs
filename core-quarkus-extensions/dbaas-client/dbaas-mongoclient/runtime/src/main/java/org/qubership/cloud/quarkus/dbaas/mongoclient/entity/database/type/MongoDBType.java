package org.qubership.cloud.quarkus.dbaas.mongoclient.entity.database.type;

import org.qubership.cloud.dbaas.client.entity.database.type.DatabaseType;
import org.qubership.cloud.quarkus.dbaas.mongoclient.entity.connection.MongoDBConnection;
import org.qubership.cloud.quarkus.dbaas.mongoclient.entity.database.MongoDatabase;

/**
 * The class used to invoke the API of {@link org.qubership.cloud.dbaas.client.DbaasClient}
 * which can operate with mongodb database
 *
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
        super("mongodb", MongoDatabase.class);
    }
}
