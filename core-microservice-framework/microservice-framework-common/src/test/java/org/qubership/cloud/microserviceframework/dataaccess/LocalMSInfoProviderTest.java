package org.qubership.cloud.microserviceframework.dataaccess;

import org.qubership.cloud.dbaas.client.config.MSInfoProvider;
import org.qubership.cloud.microserviceframework.config.annotation.EnableMSInfoProvider;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;

public class LocalMSInfoProviderTest {
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
    public void testLocalDev() throws Exception {
        initContext(LocalDevConfigTest.class);

        reinitializeMSInfo();

        assertEquals(null, msInfoProvider.getNamespace());
        assertEquals(msInfoProvider.getMicroserviceName(), MICROSERVICE_NAME);
    }

    @Test
    public void testSandbox() throws Exception {
        initContext(SandboxConfigTest.class);

        reinitializeMSInfo();

        assertEquals(msInfoProvider.getNamespace(), SANDBOX_NAMESPACE);
        assertEquals(msInfoProvider.getMicroserviceName(), MICROSERVICE_NAME);
    }

    @Test
    public void testDefaultNamespace() throws Exception {
        initContext(LocalDevDefaultNamespaceConfigTest.class);

        reinitializeMSInfo();

        assertEquals(null, msInfoProvider.getNamespace());
        assertEquals(MICROSERVICE_NAME, msInfoProvider.getMicroserviceName());
    }

    @Test
    public void testEmptyNamespace() throws Exception {
        initContext(LocalDevEmptyNamespaceConfigTest.class);

        reinitializeMSInfo();

        assertEquals(null, msInfoProvider.getNamespace());
        assertEquals(MICROSERVICE_NAME, msInfoProvider.getMicroserviceName());
    }


    @After
    public void tearDown() throws Exception {
        applicationContext.close();
    }

    @RunWith(SpringJUnit4ClassRunner.class)
    @EnableMSInfoProvider
    @TestPropertySource(properties = {
            "cloud.microservice.namespace=unknown",
            "cloud.microservice.name=" + MICROSERVICE_NAME
    })
    private static class LocalDevConfigTest { }

    @RunWith(SpringJUnit4ClassRunner.class)
    @EnableMSInfoProvider
    @TestPropertySource(properties = {
            "cloud.microservice.namespace=" + SANDBOX_NAMESPACE,
            "cloud.microservice.name=" + MICROSERVICE_NAME
    })
    private static class SandboxConfigTest {}


    @RunWith(SpringJUnit4ClassRunner.class)
    @EnableMSInfoProvider
    @TestPropertySource(properties = {
            "cloud.microservice.namespace=default",
            "cloud.microservice.name=" + MICROSERVICE_NAME
    })
    private static class LocalDevDefaultNamespaceConfigTest {
    }

    @RunWith(SpringJUnit4ClassRunner.class)
    @EnableMSInfoProvider
    @TestPropertySource(properties = {
            "cloud.microservice.namespace= ",
            "cloud.microservice.name=" + MICROSERVICE_NAME
    })
    private static class LocalDevEmptyNamespaceConfigTest {
    }
}
