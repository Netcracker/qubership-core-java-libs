package com.netcracker.cloud.dbaas.client.redis.connection;

import org.qubership.cloud.dbaas.client.management.DatabaseConfig;
import org.qubership.cloud.dbaas.client.management.DatabasePool;
import org.qubership.cloud.dbaas.client.management.classifier.DbaaSChainClassifierBuilder;
import org.qubership.cloud.dbaas.client.redis.entity.database.RedisDatabase;
import org.qubership.cloud.dbaas.client.redis.entity.database.type.RedisDBType;
import lombok.AllArgsConstructor;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisClusterConnection;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisSentinelConnection;

@AllArgsConstructor
public class DbaasRedisConnectionFactory implements RedisConnectionFactory {

    private DatabasePool databasePool;
    private DbaaSChainClassifierBuilder classifierBuilder;
    private DatabaseConfig databaseConfig;

    @Override
    public RedisConnection getConnection() {
        return getFactory().getConnection();
    }

    @Override
    public RedisClusterConnection getClusterConnection() {
        return getFactory().getClusterConnection();
    }

    @Override
    public boolean getConvertPipelineAndTxResults() {
        return getFactory().getConvertPipelineAndTxResults();
    }

    @Override
    public RedisSentinelConnection getSentinelConnection() {
        return getFactory().getSentinelConnection();
    }

    @Override
    public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
        return getFactory().translateExceptionIfPossible(ex);
    }

    protected RedisConnectionFactory getFactory() {
        RedisDatabase database = databasePool.getOrCreateDatabase(RedisDBType.INSTANCE, classifierBuilder.build(), databaseConfig);
        return database.getConnectionProperties().getConnection();
    }
}
