package com.netcracker.cloud.dbaas.client.redis.test.configuration;

import org.qubership.cloud.dbaas.client.DbaasClient;
import org.qubership.cloud.dbaas.client.management.DatabaseConfig;
import org.qubership.cloud.dbaas.client.redis.entity.connection.RedisDBConnection;
import org.qubership.cloud.dbaas.client.redis.entity.database.RedisDatabase;
import lombok.extern.slf4j.Slf4j;

import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.HashMap;

import static org.qubership.cloud.dbaas.client.redis.test.RedisTestCommon.TEST_DB_PORT;
import static org.qubership.cloud.dbaas.client.redis.test.RedisTestCommon.TEST_PASSWORD;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Configuration
@Slf4j
public class TestRedisConfiguration {

    public static final String DB_NAME_SERVICE = "db-test-name-service";
    public static final String DB_NAME_TENANT = "db-test-name-tenant";

    @Bean
    public TestRedisContainer getContainer() {
        TestRedisContainer container = TestRedisContainer.getInstance();
        container.start();
        return container;
    }

    @Bean
    @Primary
    public DbaasClient dbaasClient(TestRedisContainer container) {
        DbaasClient dbaasClient = Mockito.mock(DbaasClient.class);
        when(dbaasClient.getOrCreateDatabase(any(), any(), any(), any(DatabaseConfig.class)))
                .thenAnswer((Answer<RedisDatabase>) invocationOnMock -> {
                    HashMap<String, String> classifierFromMock = (HashMap<String, String>) invocationOnMock.getArguments()[2];
                    return getRedisDatabase(container);
                });
        return dbaasClient;
    }

    public RedisDatabase getRedisDatabase(TestRedisContainer container) {
        RedisDatabase redisDatabase = new RedisDatabase();
        redisDatabase.setName(DB_NAME_SERVICE);

        RedisDBConnection connection = new RedisDBConnection();
        String host = container.getHost();
        Integer port = container.getMappedPort(TEST_DB_PORT);
        connection.setHost(host);
        connection.setPort(port);
        connection.setPassword(TEST_PASSWORD);

        redisDatabase.setConnectionProperties(connection);
        log.debug("Returning RedisDatabase from DbaasClient. RedisDBConnection: {}.", connection);
        return redisDatabase;
    }
}
