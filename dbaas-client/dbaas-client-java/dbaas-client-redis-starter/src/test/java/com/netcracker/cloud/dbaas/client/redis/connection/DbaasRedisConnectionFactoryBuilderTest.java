package com.netcracker.cloud.dbaas.client.redis.connection;

import com.netcracker.cloud.framework.contexts.tenant.context.TenantContext;
import com.netcracker.cloud.dbaas.client.management.classifier.DbaaSChainClassifierBuilder;
import com.netcracker.cloud.dbaas.client.redis.test.configuration.MockedRedisDBConfiguration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.netcracker.cloud.dbaas.client.redis.test.RedisTestCommon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {MockedRedisDBConfiguration.class})
class DbaasRedisConnectionFactoryBuilderTest {
    private static final String FIRST_TENANT = "firstTenant";
    private static final String SECOND_TENANT = "secondTenant";

    @Autowired
    DbaasRedisConnectionFactoryBuilder builder;

    @Test
    void testBuildServiceDbFactory() {
        DbaaSChainClassifierBuilder serviceClassifierBuilder = RedisTestCommon.TEST_CLASSIFIER_FACTORY.newServiceClassifierBuilder();
        DbaasRedisConnectionFactory dbaasServiceConnectionFactory = builder.newBuilder(serviceClassifierBuilder).withDatabaseConfig(RedisTestCommon.TEST_DATABASE_CONFIG).build();
        RedisConnectionFactory redisConnectionFactoryFirst = dbaasServiceConnectionFactory.getFactory();
        assertNotNull(redisConnectionFactoryFirst);
        RedisConnectionFactory redisConnectionFactorySecond = dbaasServiceConnectionFactory.getFactory();
        assertNotNull(redisConnectionFactorySecond);
        assertEquals(redisConnectionFactoryFirst, redisConnectionFactorySecond);
    }

    @Test
    void testBuildTenantDbFactory() {
        DbaaSChainClassifierBuilder tenantClassifierBuilder = RedisTestCommon.TEST_CLASSIFIER_FACTORY.newTenantClassifierBuilder();
        DbaasRedisConnectionFactory dbaasTenantConnectionFactory = builder.newBuilder(tenantClassifierBuilder).withDatabaseConfig(RedisTestCommon.TEST_DATABASE_CONFIG).build();
        TenantContext.set(FIRST_TENANT);
        RedisConnectionFactory firstTenantFactoryA = dbaasTenantConnectionFactory.getFactory();
        assertNotNull(firstTenantFactoryA);
        RedisConnectionFactory firstTenantFactoryB = dbaasTenantConnectionFactory.getFactory();
        assertNotNull(firstTenantFactoryB);
        assertEquals(firstTenantFactoryA, firstTenantFactoryB);
        TenantContext.set(SECOND_TENANT);
        RedisConnectionFactory secondTenantFactory = dbaasTenantConnectionFactory.getFactory();
        assertNotEquals(firstTenantFactoryA, secondTenantFactory);
    }
}
