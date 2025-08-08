package com.netcracker.cloud.dbaas.client.redis.test;

import com.netcracker.cloud.dbaas.client.config.MSInfoProvider;
import com.netcracker.cloud.dbaas.client.management.DatabaseConfig;
import com.netcracker.cloud.dbaas.client.management.classifier.DbaasClassifierFactory;
import com.netcracker.cloud.dbaas.client.redis.entity.connection.RedisDBConnection;
import com.netcracker.cloud.dbaas.client.redis.entity.database.RedisDatabase;

public class RedisTestCommon {
    public static final String TEST_MICROSERVICE_NAME = "test-microservice";
    public static final String TEST_NAMESPACE = "test-namespace";

    public static final String TEST_DB_NAME = "db-test-name";
    public static final String TEST_DB_HOST = "test-redis-host";
    public static final String TEST_SERVICE = "test-service";
    public static final String TEST_PASSWORD = "test-password";
    public static final int TEST_DB_PORT = 6379;

    public static final DbaasClassifierFactory TEST_CLASSIFIER_FACTORY = new DbaasClassifierFactory(new MSInfoProvider() {
        @Override
        public String getMicroserviceName() {
            return TEST_MICROSERVICE_NAME;
        }

        @Override
        public String getNamespace() {
            return TEST_NAMESPACE;
        }

        @Override
        public String getLocalDevNamespace() {
            return TEST_NAMESPACE;
        }
    });
    public static final DatabaseConfig TEST_DATABASE_CONFIG = DatabaseConfig.builder().userRole("test-role").dbNamePrefix("test-prefix").build();

    public static RedisDatabase createRedisDatabase(String dbName, String dbHost, int dbPort, String password) {
        RedisDatabase database = new RedisDatabase();
        database.setName(dbName);
        RedisDBConnection redisDBConnection = new RedisDBConnection();
        redisDBConnection.setHost(dbHost);
        redisDBConnection.setPort(dbPort);
        redisDBConnection.setPassword(password);
        redisDBConnection.setService(TEST_SERVICE);
        database.setConnectionProperties(redisDBConnection);
        return database;
    }

}
