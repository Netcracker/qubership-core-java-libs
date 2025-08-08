package com.netcracker.cloud.dbaas.client.redis.configuration;

import org.qubership.cloud.dbaas.client.entity.DbaasApiProperties;
import org.qubership.cloud.dbaas.client.management.DatabasePool;
import org.qubership.cloud.dbaas.client.redis.test.configuration.MockedRedisDBConfiguration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {MockedRedisDBConfiguration.class})
@TestPropertySource(properties = {
        "dbaas.redis.timeout=100",
        "dbaas.redis.connectTimeout=200",
        "dbaas.redis.clientName=test-client",
        "dbaas.api.redis.db-prefix=test-prefix",
        "dbaas.api.redis.runtime-user-role=admin"
})
class DbaasRedisDBConfigurationTest {

    @SpyBean
    private DatabasePool databasePool;

    @Autowired
    private ApplicationContext applicationContext;


    @Autowired
    private RedisProperties redisProperties;

    @Autowired
    private DbaasApiProperties redisDbaasApiProperties;

    @Test
    void checkRedisConfigLoaded() {
        assertEquals(Duration.ofMillis(100), redisProperties.getTimeout());
        assertEquals(Duration.ofMillis(200), redisProperties.getConnectTimeout());
        assertEquals("test-client", redisProperties.getClientName());
    }

    @Test
    void checkRedisApiProperties() {
        assertEquals("admin", redisDbaasApiProperties.getRuntimeUserRole());
        assertEquals("test-prefix", redisDbaasApiProperties.getDbPrefix());
    }

}
