package org.qubership.cloud.dbaas.client.redis.configuration;

import org.qubership.cloud.dbaas.client.config.EnableDbaasDefault;
import org.qubership.cloud.dbaas.client.entity.DbaasApiProperties;
import org.qubership.cloud.dbaas.client.management.DatabasePool;
import org.qubership.cloud.dbaas.client.redis.connection.DbaasRedisConnectionFactoryBuilder;
import org.qubership.cloud.dbaas.client.redis.management.JedisClientCreator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@EnableDbaasDefault
@Configuration
@EnableConfigurationProperties
public class DbaasRedisConfiguration {

    @Bean("redisDbaasApiProperties")
    @ConfigurationProperties("dbaas.api.redis")
    public DbaasApiProperties dbaasApiProperties() {
        return new DbaasApiProperties();
    }

    @Bean
    @Primary
    @ConfigurationProperties("dbaas.redis")
    public RedisProperties redisProperties() {
        return new RedisProperties();
    }

    @Bean
    @ConditionalOnMissingBean
    public JedisClientCreator jedisClientCreator(
            RedisProperties redisProperties) {
        return new JedisClientCreator(redisProperties);
    }

    @Bean
    public DbaasRedisConnectionFactoryBuilder dbaasRedisConnectionFactoryBuilder(DatabasePool databasePool) {
        return new DbaasRedisConnectionFactoryBuilder(databasePool);
    }
}
