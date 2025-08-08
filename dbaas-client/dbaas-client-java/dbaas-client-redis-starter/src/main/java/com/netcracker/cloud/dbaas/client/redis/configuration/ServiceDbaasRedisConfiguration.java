package org.qubership.cloud.dbaas.client.redis.configuration;

import org.qubership.cloud.dbaas.client.entity.DbaasApiProperties;
import org.qubership.cloud.dbaas.client.management.DatabaseConfig;
import org.qubership.cloud.dbaas.client.management.classifier.DbaasClassifierFactory;
import org.qubership.cloud.dbaas.client.redis.connection.DbaasRedisConnectionFactoryBuilder;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
@Import(DbaasRedisConfiguration.class)
public class ServiceDbaasRedisConfiguration {

    public static final String SERVICE_DBAAS_REDIS_CONNECTION_FACTORY = "serviceDbaasRedisConnectionFactory";
    public static final String SERVICE_DBAAS_REDIS_TEMPLATE = "serviceDbaasRedisTemplate";

    @Bean(SERVICE_DBAAS_REDIS_CONNECTION_FACTORY)
    @ConditionalOnMissingBean(name = SERVICE_DBAAS_REDIS_CONNECTION_FACTORY)
    public RedisConnectionFactory serviceRedisConnectionFactory(DbaasRedisConnectionFactoryBuilder builder,
                                                                DbaasApiProperties redisDbaasApiProperties,
                                                                DbaasClassifierFactory classifierFactory) {
        DatabaseConfig databaseConfig = DatabaseConfig.builder()
                .dbNamePrefix(redisDbaasApiProperties.getDbPrefix())
                .build();

        return builder.newBuilder(classifierFactory.newServiceClassifierBuilder()).withDatabaseConfig(databaseConfig).build();
    }

    @Bean(SERVICE_DBAAS_REDIS_TEMPLATE)
    @ConditionalOnMissingBean(name = SERVICE_DBAAS_REDIS_TEMPLATE)
    public RedisTemplate<String, Object> serviceRedisTemplate(@Qualifier(SERVICE_DBAAS_REDIS_CONNECTION_FACTORY) RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);
        return redisTemplate;
    }
}
