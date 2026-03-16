package com.netcracker.cloud.quarkus.dbaas.mongoclient.entity.database.type;

import com.netcracker.cloud.dbaas.client.entity.database.type.DatabaseType;
import com.netcracker.cloud.quarkus.dbaas.mongoclient.entity.connection.MongoDBConnection;
import com.netcracker.cloud.quarkus.dbaas.mongoclient.entity.database.MongoDatabase;

/**
 * The class used to invoke the API of {@link com.netcracker.cloud.dbaas.client.DbaasClient}
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
