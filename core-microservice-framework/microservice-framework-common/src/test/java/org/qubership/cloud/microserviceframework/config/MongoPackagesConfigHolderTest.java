package org.qubership.cloud.microserviceframework.config;

import org.qubership.cloud.microserviceframework.config.annotation.EnableCustomChangeLogsPackages;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import static org.qubership.cloud.microserviceframework.testconfig.TestConstants.*;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
public class MongoPackagesConfigHolderTest {

    private MongoPackagesConfigHolder configHolder = MongoPackagesConfigHolder.getInstance();

    @Test
    public void setTenantAwareCustomPackage() {

        assertNull(configHolder.getTenantAwareCustomPackage());
        configHolder.setUsingCustomPackages(false);
        configHolder.setTenantAwareCustomPackage(TEST_MONGO_PACKAGE);
        assertTrue(configHolder.isUsingCustomPackages());
        assertEquals(configHolder.getTenantAwareCustomPackage(), TEST_MONGO_PACKAGE);
    }

    @Test
    public void setMicroserviceCustomPackage() {
        assertNull(configHolder.getMicroserviceCustomPackage());
        configHolder.setUsingCustomPackages(false);
        configHolder.setMicroserviceCustomPackage(TEST_MONGO_PACKAGE);
        assertTrue(configHolder.isUsingCustomPackages());
        assertEquals(configHolder.getMicroserviceCustomPackage(), TEST_MONGO_PACKAGE);
    }

    @Test
    public void testChangeLogsPackages() throws Exception {
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