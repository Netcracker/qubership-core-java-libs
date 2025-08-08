package com.netcracker.cloud.dbaas.client.redis.entity.database.type;

import org.qubership.cloud.dbaas.client.redis.entity.connection.RedisDBConnection;
import org.qubership.cloud.dbaas.client.redis.entity.database.RedisDatabase;

public class RedisDBType extends AbstractRedisDBType<RedisDBConnection, RedisDatabase> {

    public static final RedisDBType INSTANCE = new RedisDBType(RedisDatabase.class);

    private RedisDBType(Class<RedisDatabase> databaseClass) {
        super(databaseClass);
    }

}
