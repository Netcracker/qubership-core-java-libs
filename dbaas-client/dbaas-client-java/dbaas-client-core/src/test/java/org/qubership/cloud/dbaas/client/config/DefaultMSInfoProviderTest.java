package org.qubership.cloud.dbaas.client.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DefaultMSInfoProviderTest {
    static final String BASELINE_NAMESPACE = "cloud-catalog-test";
    static final String SANDBOX_NAMESPACE = "cloud-catalog-sandbox07";
    static final String LOCALDEV_NAMESPACE = "127.0.0.1.xip.io";
    static final String MICROSERVICE_NAME = "test-ms";

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    @Autowired
    private MSInfoProvider msInfoProvider;

    private void initContext(Class<?> testClass) throws Exception {
        TestContextManager testContextManager = new TestContextManager(testClass);
        testContextManager.prepareTestInstance(this);
    }

    @Test
    public void testLocalDev() throws Exception {
        initContext(LocalDevConfigTest.class);

        assertEquals(BASELINE_NAMESPACE,msInfoProvider.getNamespace());
        assertEquals(MICROSERVICE_NAME,msInfoProvider.getMicroserviceName());
    }

    @Test
    public void testSandbox() throws Exception {
        initContext(SandboxConfigTest.class);

        assertEquals(SANDBOX_NAMESPACE, msInfoProvider.getNamespace());
        assertEquals(MICROSERVICE_NAME, msInfoProvider.getMicroserviceName());
    }

    @AfterEach
    public void tearDown() throws Exception {
        applicationContext.close();
    }

    @ExtendWith(SpringExtension.class)
    @Import(TestMSInfoProviderConfig.class)
    @TestPropertySource(properties = {
            "dbaas.core.localdev=" + LOCALDEV_NAMESPACE,
            "cloud.microservice.namespace=" + BASELINE_NAMESPACE,
            "cloud.microservice.name=" + MICROSERVICE_NAME
    })
    private static class LocalDevConfigTest {
    }

    @ExtendWith(SpringExtension.class)
    @Import(TestMSInfoProviderConfig.class)
    @TestPropertySource(properties = {
            "cloud.microservice.namespace=" + SANDBOX_NAMESPACE,
            "cloud.microservice.name=" + MICROSERVICE_NAME
    })
    private static class SandboxConfigTest {
    }
}
