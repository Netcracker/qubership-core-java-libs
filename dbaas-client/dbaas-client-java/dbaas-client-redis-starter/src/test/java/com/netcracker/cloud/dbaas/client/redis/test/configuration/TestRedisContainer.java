package org.qubership.cloud.dbaas.client.redis.test.configuration;

import jakarta.annotation.PreDestroy;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import static org.qubership.cloud.dbaas.client.redis.test.RedisTestCommon.TEST_DB_PORT;

public class TestRedisContainer extends GenericContainer<TestRedisContainer> {
    private static final DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:7.4.1-alpine3.20");

    private static TestRedisContainer redisTestContainer;

    private TestRedisContainer() {
        super(REDIS_IMAGE);
    }

    @PreDestroy
    public void destroy() {
        redisTestContainer.stop();
    }

    public static TestRedisContainer getInstance() {
        if (redisTestContainer == null) {
            redisTestContainer = new TestRedisContainer()
                    .withExposedPorts(TEST_DB_PORT)
                    .withCopyToContainer(MountableFile.forClasspathResource("redis/redis.conf"), "/etc/redis/redis.conf")
                    .withCommand("/etc/redis/redis.conf", "--requirepass test-password");
        }
        return redisTestContainer;
    }
}