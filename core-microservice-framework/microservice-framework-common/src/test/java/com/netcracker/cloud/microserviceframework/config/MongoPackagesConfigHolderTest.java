package com.netcracker.cloud.microserviceframework.config;

import org.junit.jupiter.api.Test;
import com.netcracker.cloud.microserviceframework.config.annotation.EnableCustomChangeLogsPackages;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import static org.junit.jupiter.api.Assertions.*;
import static com.netcracker.cloud.microserviceframework.testconfig.TestConstants.*;

class MongoPackagesConfigHolderTest {

    private MongoPackagesConfigHolder configHolder = MongoPackagesConfigHolder.getInstance();

    @Test
    void setTenantAwareCustomPackage() {

        assertNull(configHolder.getTenantAwareCustomPackage());
        configHolder.setUsingCustomPackages(false);
        configHolder.setTenantAwareCustomPackage(TEST_MONGO_PACKAGE);
        assertTrue(configHolder.isUsingCustomPackages());
        assertEquals(TEST_MONGO_PACKAGE, configHolder.getTenantAwareCustomPackage());
    }

    @Test
    void setMicroserviceCustomPackage() {
        assertNull(configHolder.getMicroserviceCustomPackage());
        configHolder.setUsingCustomPackages(false);
        configHolder.setMicroserviceCustomPackage(TEST_MONGO_PACKAGE);
        assertTrue(configHolder.isUsingCustomPackages());
        assertEquals(configHolder.getMicroserviceCustomPackage(), TEST_MONGO_PACKAGE);
    }

    @Test
    void testChangeLogsPackages() throws Exception {
        init(MongoEvolutionConfigurationClassForTest.class);
        assertEquals(MS_CHANGE_LOGS_PACKAGE,configHolder.getMicroserviceCustomPackage());
        assertEquals(TENANT_AWARE_CHANGE_LOGS_PACKAGE,configHolder.getTenantAwareCustomPackage());
    }

    @EnableCustomChangeLogsPackages(
            microserviceChangeLogsPackage = MS_CHANGE_LOGS_PACKAGE,
            tenantAwareChangeLogsPackage = TENANT_AWARE_CHANGE_LOGS_PACKAGE)
    @ContextConfiguration(classes=MongoEvolutionConfiguration.class, loader= AnnotationConfigContextLoader.class)
    public static class MongoEvolutionConfigurationClassForTest{}

    private void init(Class<?> className) throws Exception {
        TestContextManager testContextManager = new TestContextManager(className);
        testContextManager.prepareTestInstance(this);
    }
}
