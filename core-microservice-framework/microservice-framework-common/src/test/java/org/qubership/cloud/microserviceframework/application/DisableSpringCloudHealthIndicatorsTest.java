package org.qubership.cloud.microserviceframework.application;


import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.qubership.cloud.microserviceframework.testconfig.TestConstants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DisableSpringCloudHealthIndicatorsTest {

    @Autowired
    private ConfigurableEnvironment env;

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    private void initContext(Class<?> testClass) throws Exception {
        TestContextManager testContextManager = new TestContextManager(testClass);
        testContextManager.prepareTestInstance(this);
    }

    @Test
    public void defaultDiscoveryPropertyWithDefaultValueTest() throws Exception {
        initContext(ThereIsNoSpringCloudPropertyValue.class);
        assertNotNull(env.containsProperty(DISCOVERY_PROPERTY_NAME));
        assertEquals(FALSE_VALUE, env.getProperty(DISCOVERY_PROPERTY_NAME));
    }

    @Test
    public void defaultDiscoveryPropertyWithCustomValueTest() throws Exception {
        initContext(ThereIsSpringCloudPropertyValue.class);
        assertNotNull(env.containsProperty(DISCOVERY_PROPERTY_NAME));
        assertEquals(TRUE_VALUE, env.getProperty(DISCOVERY_PROPERTY_NAME));
    }

    @After
    public void tearDown() {
        applicationContext.close();
    }

    @RunWith(SpringJUnit4ClassRunner.class)
    @SpringBootTest(classes = DisableSpringCloudHealthIndicators.class)
    @TestPropertySource(properties = {
            "spring.cloud.config.enabled=" + false
    })
    private static class ThereIsNoSpringCloudPropertyValue {}

    @RunWith(SpringJUnit4ClassRunner.class)
    @SpringBootTest(classes = DisableSpringCloudHealthIndicators.class)
    @TestPropertySource(properties = {
            DISCOVERY_PROPERTY,
            "spring.cloud.config.enabled=" + false
    })
    private static class ThereIsSpringCloudPropertyValue {}
}
