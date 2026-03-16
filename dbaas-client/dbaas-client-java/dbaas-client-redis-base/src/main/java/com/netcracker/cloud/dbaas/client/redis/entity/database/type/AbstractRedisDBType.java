package com.netcracker.cloud.dbaas.client.redis.entity.database.type;

import com.netcracker.cloud.dbaas.client.entity.database.type.DatabaseType;
import com.netcracker.cloud.dbaas.client.redis.entity.connection.AbstractRedisDBConnection;
import com.netcracker.cloud.dbaas.client.redis.entity.database.AbstractRedisDatabase;

import static com.netcracker.cloud.dbaas.client.entity.database.type.PhysicalDbType.REDIS;

public abstract class AbstractRedisDBType<C extends AbstractRedisDBConnection, D extends AbstractRedisDatabase<C>> extends DatabaseType<C, D> {

    protected AbstractRedisDBType(Class<? extends D> databaseClass) {
        super(REDIS, databaseClass);
    }

}
