package com.netcracker.cloud.junit.cloudcore.extension.callbacks;

import com.netcracker.cloud.junit.cloudcore.extension.callbacks.classes.TestClass;
import com.netcracker.cloud.junit.cloudcore.extension.callbacks.classes.TestClassLev2;
import com.netcracker.cloud.junit.cloudcore.extension.client.TestKubernetesClientFactory;
import com.netcracker.cloud.junit.cloudcore.extension.provider.PortForwardConfig;
import com.netcracker.cloud.junit.cloudcore.extension.provider.PortForwardServiceManager;
import com.netcracker.cloud.junit.cloudcore.extension.provider.TestPortForwardServiceManager;
import com.netcracker.cloud.junit.cloudcore.extension.service.BasePortForwardParams;
import com.netcracker.cloud.junit.cloudcore.extension.service.Endpoint;
import com.netcracker.cloud.junit.cloudcore.extension.service.NetSocketAddress;
import com.netcracker.cloud.junit.cloudcore.extension.service.PortForwardService;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.Mockito;

import java.net.URI;

public class CloudCoreJunitCallbackTest {
    String testCloud = "test-cloud";
    String testNamespace = "test-namespace";

    @Test
    void testBeforeAllTestKubernetesClient() throws Exception {
        try {
            System.setProperty("clouds.cloud_1.name", "test-cloud-1");
            System.setProperty("clouds.cloud_2.name", "test-cloud-2");
            System.setProperty("clouds.cloud_1.namespaces.origin", testNamespace);
            System.setProperty("clouds.cloud_2.namespaces.origin", testNamespace);

            ExtensionContext extensionContext = Mockito.mock(ExtensionContext.class);
            PortForwardServiceManager portForwardServiceManager = Mockito.mock(PortForwardServiceManager.class);
            KubernetesClient kubernetesClient_1 = Mockito.mock(KubernetesClient.class);
            KubernetesClient kubernetesClient_2 = Mockito.mock(KubernetesClient.class);
            PortForwardService portForwardService = Mockito.mock(PortForwardService.class);
            TestClass.TestKubernetesClient testInstance = new TestClass.TestKubernetesClient();
            Mockito.when(extensionContext.getRequiredTestInstance()).thenReturn(testInstance);
            PortForwardConfig hostnamePortForwardConfig = new PortForwardConfig(testCloud, testNamespace);
            Mockito.when(portForwardServiceManager.getPortForwardService(hostnamePortForwardConfig)).thenReturn(portForwardService);
            TestPortForwardServiceManager.setFunction(portForwardConfig -> portForwardService);
            TestKubernetesClientFactory.setFunction(cloudAndNamespace -> {
                if ("test-cloud-1".equals(cloudAndNamespace.getCloud()) && testNamespace.equals(cloudAndNamespace.getNamespace())) {
                    return kubernetesClient_1;
                } else if ("test-cloud-2".equals(cloudAndNamespace.getCloud()) && testNamespace.equals(cloudAndNamespace.getNamespace())) {
                    return kubernetesClient_2;
                } else {
                    throw new IllegalArgumentException("Unexpected cloud and namespace combination: " + cloudAndNamespace);
                }
            });

            new CloudCoreJunitCallback().beforeAll(extensionContext);

            Assertions.assertEquals(kubernetesClient_1, testInstance.getCloud1KubernetesClient());
            Assertions.assertEquals(kubernetesClient_2, testInstance.getCloud2KubernetesClient());
        } finally {
            System.clearProperty("clouds.cloud_1.name");
            System.clearProperty("clouds.cloud_2.name");
            System.clearProperty("clouds.cloud_1.namespaces.origin");
            System.clearProperty("clouds.cloud_2.namespaces.origin");
        }
    }

    @Test
    void testBeforeAllTestURI() throws Exception {
        try {
            System.setProperty("clouds.cloud.name", testCloud);
            System.setProperty("clouds.cloud.namespaces.namespace", testNamespace);

            ExtensionContext extensionContext = Mockito.mock(ExtensionContext.class);
            KubernetesClient kubernetesClient = Mockito.mock(KubernetesClient.class);
            PortForwardService portForwardService = Mockito.mock(PortForwardService.class);
            TestClass.TestUri testInstance = new TestClass.TestUri();
            Mockito.when(extensionContext.getRequiredTestInstance()).thenReturn(testInstance);
            TestPortForwardServiceManager.setFunction(c -> portForwardService);
            TestKubernetesClientFactory.setFunction(cloudAndNamespace -> kubernetesClient);
            Mockito.when(portForwardService.portForward(Mockito.any())).thenReturn(URI.create("ftp://test-host:8181/").toURL());

            new CloudCoreJunitCallback().beforeAll(extensionContext);

            Assertions.assertEquals("ftp://test-host:8181/", testInstance.getUri().toString());
        } finally {
            System.clearProperty("clouds.cloud.name");
            System.clearProperty("clouds.cloud.namespaces.namespace");
        }
    }

    @Test
    void testBeforeAllTestURL() throws Exception {
        try {
            System.setProperty("clouds.cloud.name", testCloud);
            System.setProperty("clouds.cloud.namespaces.namespace", testNamespace);

            ExtensionContext extensionContext = Mockito.mock(ExtensionContext.class);
            KubernetesClient kubernetesClient = Mockito.mock(KubernetesClient.class);
            PortForwardService portForwardService = Mockito.mock(PortForwardService.class);
            TestClass.TestUrl testInstance = new TestClass.TestUrl();
            Mockito.when(extensionContext.getRequiredTestInstance()).thenReturn(testInstance);
            TestPortForwardServiceManager.setFunction(c -> portForwardService);
            TestKubernetesClientFactory.setFunction(cloudAndNamespace -> kubernetesClient);
            Mockito.when(portForwardService.portForward(Mockito.any())).thenReturn(URI.create("https://test-host:8181/").toURL());

            new CloudCoreJunitCallback().beforeAll(extensionContext);

            Assertions.assertEquals("https://test-host:8181/", testInstance.getUrl().toString());
        } finally {
            System.clearProperty("clouds.cloud.name");
            System.clearProperty("clouds.cloud.namespaces.namespace");
        }
    }

    @Test
    void testBeforeAllTestTestPortForwardService() throws Exception {
        try {
            System.setProperty("clouds.cloud.name", testCloud);
            System.setProperty("clouds.cloud.namespaces.namespace", testNamespace);

            ExtensionContext extensionContext = Mockito.mock(ExtensionContext.class);
            KubernetesClient kubernetesClient = Mockito.mock(KubernetesClient.class);
            PortForwardService portForwardService = Mockito.mock(PortForwardService.class);
            TestClass.TestPortForwardService testInstance = new TestClass.TestPortForwardService();
            Mockito.when(extensionContext.getRequiredTestInstance()).thenReturn(testInstance);
            TestPortForwardServiceManager.setFunction(c -> portForwardService);
            TestKubernetesClientFactory.setFunction(cloudAndNamespace -> kubernetesClient);
            Mockito.when(portForwardService.portForward(Mockito.any())).thenReturn(new NetSocketAddress("test-host", 8181));

            new CloudCoreJunitCallback().beforeAll(extensionContext);

            Assertions.assertNotNull(testInstance.getPortForwardService());
        } finally {
            System.clearProperty("clouds.cloud.name");
            System.clearProperty("clouds.cloud.namespaces.namespace");
        }
    }

    @Test
    void testBeforeAllTestNetSocketAddress() throws Exception {
        try {
            System.setProperty("clouds.cloud_1.name", testCloud);
            System.setProperty("clouds.cloud_1.namespaces.origin", testNamespace);

            ExtensionContext extensionContext = Mockito.mock(ExtensionContext.class);
            PortForwardServiceManager resourceFactory = Mockito.mock(PortForwardServiceManager.class);
            KubernetesClient kubernetesClient = Mockito.mock(KubernetesClient.class);
            PortForwardService portForwardService = Mockito.mock(PortForwardService.class);
            TestClass.TestSocketAddress testInstance = new TestClass.TestSocketAddress();
            Mockito.when(extensionContext.getRequiredTestInstance()).thenReturn(testInstance);
            TestPortForwardServiceManager.setFunction(c -> portForwardService);
            TestKubernetesClientFactory.setFunction(cloudAndNamespace -> kubernetesClient);
            Mockito.when(portForwardService.portForward(Mockito.any())).thenAnswer(i -> {
                BasePortForwardParams params = i.getArgument(0);
                return new NetSocketAddress(params.getName(), params.getPort());
            });

            new CloudCoreJunitCallback().beforeAll(extensionContext);

            NetSocketAddress address = testInstance.getAddress();
            Assertions.assertEquals("postgres:5432", address.getEndpoint());
        } finally {
            System.clearProperty("clouds.cloud_1.name");
            System.clearProperty("clouds.cloud_1.namespaces.origin");
        }
    }

    @Test
    void testAfterAll() throws Exception {
        try {
            System.setProperty("clouds.cloud.name", testCloud);
            System.setProperty("clouds.cloud.namespaces.namespace", testNamespace);

            ExtensionContext extensionContext = Mockito.mock(ExtensionContext.class);
            URI url1 = URI.create("http://test-1:8080/");
            URI url2 = URI.create("http://test-2:8080/");
            Object testInstance = new TestClassLev2(url1, url2);
            Mockito.when(extensionContext.getRequiredTestInstance()).thenReturn(testInstance);
            PortForwardService portForwardService = Mockito.mock(PortForwardService.class);
            TestPortForwardServiceManager.setFunction(c -> portForwardService);

            new CloudCoreJunitCallback().afterAll(extensionContext);

            Mockito.verify(portForwardService).closePortForward(Mockito.eq(new Endpoint(url1.getHost(), url1.getPort())));
            Mockito.verify(portForwardService).closePortForward(Mockito.eq(new Endpoint(url2.getHost(), url2.getPort())));
            Assertions.assertTrue(TestPortForwardServiceManager.isClosed());
        } finally {
            System.clearProperty("clouds.cloud.name");
            System.clearProperty("clouds.cloud.namespaces.namespace");
        }
    }

    @Test
    void testAfterAllAsEnclosingClass() throws Exception {
        try {
            System.setProperty("clouds.cloud.name", testCloud);
            System.setProperty("clouds.cloud.namespaces.namespace", testNamespace);

            ExtensionContext extensionContext = Mockito.mock(ExtensionContext.class);
            URI url = Mockito.mock(URI.class);
            Object testInstance = new TestClassLev2.TestInnerClass(url);
            Mockito.when(extensionContext.getRequiredTestInstance()).thenReturn(testInstance);
            PortForwardService portForwardService = Mockito.mock(PortForwardService.class);
            TestPortForwardServiceManager.setFunction(c -> portForwardService);

            new CloudCoreJunitCallback().afterAll(extensionContext);

            Mockito.verify(portForwardService).closePortForward(new Endpoint(url.getHost(), url.getPort()));
            Assertions.assertFalse(TestPortForwardServiceManager.isClosed());
        } finally {
            System.clearProperty("clouds.cloud.name");
            System.clearProperty("clouds.cloud.namespaces.namespace");
        }
    }
}
