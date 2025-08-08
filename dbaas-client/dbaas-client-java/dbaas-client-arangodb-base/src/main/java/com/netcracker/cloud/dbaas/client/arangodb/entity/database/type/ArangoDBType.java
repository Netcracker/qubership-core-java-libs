package com.netcracker.cloud.dbaas.client.arangodb.entity.database.type;

import com.netcracker.cloud.dbaas.client.arangodb.entity.connection.ArangoConnection;
import com.netcracker.cloud.dbaas.client.arangodb.entity.database.ArangoDatabase;
import com.netcracker.cloud.dbaas.client.entity.database.type.DatabaseType;

import static com.netcracker.cloud.dbaas.client.entity.database.type.PhysicalDbType.ARANGODB;

public class ArangoDBType extends DatabaseType<ArangoConnection, ArangoDatabase> {

    public static final ArangoDBType INSTANCE = new ArangoDBType(ArangoDatabase.class);

    private ArangoDBType(Class<? extends ArangoDatabase> databaseClass) {
        super(ARANGODB, databaseClass);
    }

}
