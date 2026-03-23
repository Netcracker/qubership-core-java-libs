package com.netcracker.cloud.dbaas.client.redis.management;

import com.netcracker.cloud.dbaas.client.management.DatabaseClientCreator;
import com.netcracker.cloud.dbaas.client.redis.entity.connection.RedisDBConnection;
import com.netcracker.cloud.dbaas.client.redis.entity.database.RedisConnectorSettings;
import com.netcracker.cloud.dbaas.client.redis.entity.database.RedisDatabase;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import redis.clients.jedis.JedisPoolConfig;

@AllArgsConstructor
@Slf4j
public class JedisClientCreator implements DatabaseClientCreator<RedisDatabase, RedisConnectorSettings> {

    private DataRedisProperties redisProperties;

    @Override
    public void create(RedisDatabase database) {
        create(database, null);
    }

    @Override
    public void create(RedisDatabase database, RedisConnectorSettings settings) {
        RedisDBConnection connectionProperties = database.getConnectionProperties();
        String password = connectionProperties.getPassword();

        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration(connectionProperties.getHost(), connectionProperties.getPort());
        redisStandaloneConfiguration.setPassword(password);

        JedisClientConfiguration.JedisClientConfigurationBuilder configBuilder = JedisClientConfiguration.builder();
        applyPropertiesConfiguration(configBuilder);
        JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory(redisStandaloneConfiguration, configBuilder.build());
        jedisConnectionFactory.afterPropertiesSet();
        connectionProperties.setConnection(jedisConnectionFactory);
    }

    @Override
    public Class<RedisDatabase> getSupportedDatabaseType() {
        return RedisDatabase.class;
    }

    private void applyPropertiesConfiguration(JedisClientConfiguration.JedisClientConfigurationBuilder configBuilder) {
        PropertyMapper mapper = PropertyMapper.get();
        mapper.from(redisProperties.getSsl().isEnabled()).whenTrue().toCall(configBuilder::useSsl);
        mapper.from(redisProperties.getTimeout()).to(configBuilder::readTimeout);
        mapper.from(redisProperties.getConnectTimeout()).to(configBuilder::connectTimeout);
        mapper.from(redisProperties.getClientName()).to(configBuilder::clientName);
        mapper.from(redisProperties.getJedis().getPool()).to(pool -> applyPoolConfiguration(configBuilder, mapper, pool));
    }

    private void applyPoolConfiguration(JedisClientConfiguration.JedisClientConfigurationBuilder clientConfigurationBuilder, PropertyMapper mapper, DataRedisProperties.Pool pool) {
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        mapper.from(pool.getMaxActive()).to(jedisPoolConfig::setMaxTotal);
        mapper.from(pool.getMaxIdle()).to(jedisPoolConfig::setMaxIdle);
        mapper.from(pool.getMinIdle()).to(jedisPoolConfig::setMinIdle);
        mapper.from(pool.getTimeBetweenEvictionRuns()).to(jedisPoolConfig::setTimeBetweenEvictionRuns);
        mapper.from(pool.getMaxWait()).to(jedisPoolConfig::setMaxWait);
        clientConfigurationBuilder.usePooling().poolConfig(jedisPoolConfig);
    }

}
