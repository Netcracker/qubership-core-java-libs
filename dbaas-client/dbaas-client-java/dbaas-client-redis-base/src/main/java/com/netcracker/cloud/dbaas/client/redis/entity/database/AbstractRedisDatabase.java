package com.netcracker.cloud.dbaas.client.redis.entity.database;

import com.netcracker.cloud.dbaas.client.entity.database.AbstractDatabase;
import com.netcracker.cloud.dbaas.client.redis.entity.connection.AbstractRedisDBConnection;

public abstract class AbstractRedisDatabase<C extends AbstractRedisDBConnection> extends AbstractDatabase<C> {
}
