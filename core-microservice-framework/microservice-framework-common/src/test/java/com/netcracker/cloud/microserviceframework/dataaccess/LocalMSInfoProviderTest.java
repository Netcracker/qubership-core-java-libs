package org.qubership.cloud.microserviceframework.dataaccess;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.qubership.cloud.dbaas.client.config.MSInfoProvider;
import org.qubership.cloud.microserviceframework.config.annotation.EnableMSInfoProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.TestPropertySource;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LocalMSInfoProviderTest {
    static final String SANDBOX_NAMESPACE = "cloud-catalog-sandbox07";
    static final String MICROSERVICE_NAME = "test-ms";

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    @Autowired
    private MSInfoProvider msInfoProvider;

    private void initContext(Class<?> testClass) throws Exception {
        TestContextManager testContextManager = new TestContextManager(testClass);
        testContextManager.prepareTestInstance(this);
    }

    private void reinitializeMSInfo() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = LocalMSInfoProvider.class.getDeclaredMethod("resolveNamespace");
        method.setAccessible(true);
        method.invoke(msInfoProvider);
    }

    @Test
    void testLocalDev() throws Exception {
        initContext(LocalDevConfigTest.class);

        reinitializeMSInfo();

        assertNull(msInfoProvider.getNamespace());
        assertEquals(MICROSERVICE_NAME, msInfoProvider.getMicroserviceName());
    }

    @Test
    void testSandbox() throws Exception {
        initContext(SandboxConfigTest.class);

        reinitializeMSInfo();

        assertEquals(SANDBOX_NAMESPACE, msInfoProvider.getNamespace());
        assertEquals(MICROSERVICE_NAME, msInfoProvider.getMicroserviceName());
    }

    @Test
    void testDefaultNamespace() throws Exception {
        initContext(LocalDevDefaultNamespaceConfigTest.class);

        reinitializeMSInfo();

        assertNull(msInfoProvider.getNamespace());
        assertEquals(MICROSERVICE_NAME, msInfoProvider.getMicroserviceName());
    }

    @Test
    void testEmptyNamespace() throws Exception {
        initContext(LocalDevEmptyNamespaceConfigTest.class);

        reinitializeMSInfo();

        assertNull(msInfoProvider.getNamespace());
        assertEquals(MICROSERVICE_NAME, msInfoProvider.getMicroserviceName());
    }


    @AfterEach
    void tearDown() {
        applicationContext.close();
    }

    @EnableMSInfoProvider
    @TestPropertySource(properties = {
            "cloud.microservice.namespace=unknown",
            "cloud.microservice.name=" + MICROSERVICE_NAME
    })
    private static class LocalDevConfigTest {
    }

    @EnableMSInfoProvider
    @TestPropertySource(properties = {
            "cloud.microservice.namespace=" + SANDBOX_NAMESPACE,
            "cloud.microservice.name=" + MICROSERVICE_NAME
    })
    private static class SandboxConfigTest {
    }


    @EnableMSInfoProvider
    @TestPropertySource(properties = {
            "cloud.microservice.namespace=default",
            "cloud.microservice.name=" + MICROSERVICE_NAME
    })
    private static class LocalDevDefaultNamespaceConfigTest {
    }

    @EnableMSInfoProvider
    @TestPropertySource(properties = {
            "cloud.microservice.namespace= ",
            "cloud.microservice.name=" + MICROSERVICE_NAME
    })
    private static class LocalDevEmptyNamespaceConfigTest {
    }
}
