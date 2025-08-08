package com.netcracker.cloud.microserviceframework.application;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.qubership.cloud.microserviceframework.testconfig.TestConstants.*;

class DisableSpringCloudHealthIndicatorsTest {

    @Autowired
    private ConfigurableEnvironment env;

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    private void initContext(Class<?> testClass) throws Exception {
        TestContextManager testContextManager = new TestContextManager(testClass);
        testContextManager.prepareTestInstance(this);
    }

    @Test
    void defaultDiscoveryPropertyWithDefaultValueTest() throws Exception {
        initContext(ThereIsNoSpringCloudPropertyValue.class);
        assertNotNull(env.containsProperty(DISCOVERY_PROPERTY_NAME));
        assertEquals(FALSE_VALUE, env.getProperty(DISCOVERY_PROPERTY_NAME));
    }

    @Test
    void defaultDiscoveryPropertyWithCustomValueTest() throws Exception {
        initContext(ThereIsSpringCloudPropertyValue.class);
        assertNotNull(env.containsProperty(DISCOVERY_PROPERTY_NAME));
        assertEquals(TRUE_VALUE, env.getProperty(DISCOVERY_PROPERTY_NAME));
    }

    @AfterEach
    void tearDown() {
        applicationContext.close();
    }

    @SpringBootTest(classes = DisableSpringCloudHealthIndicators.class)
    @TestPropertySource(properties = {
            "spring.cloud.config.enabled=" + false
    })
    private static class ThereIsNoSpringCloudPropertyValue {}

    @SpringBootTest(classes = DisableSpringCloudHealthIndicators.class)
    @TestPropertySource(properties = {
            DISCOVERY_PROPERTY,
            "spring.cloud.config.enabled=" + false
    })
    private static class ThereIsSpringCloudPropertyValue {}
}
