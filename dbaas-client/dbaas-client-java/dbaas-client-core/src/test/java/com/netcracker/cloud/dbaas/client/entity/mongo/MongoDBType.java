package com.netcracker.cloud.dbaas.client.entity.mongo;

import com.netcracker.cloud.dbaas.client.entity.database.type.DatabaseType;

/**
 * Test mongodb database type.
 */
public class MongoDBType extends DatabaseType<MongoDBConnection, MongoDatabase> {

    public static final MongoDBType INSTANCE = new MongoDBType();

    private MongoDBType() {
        super("mongodb", MongoDatabase.class);
    }
}
