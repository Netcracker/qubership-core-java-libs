package org.qubership.cloud.junit.cloudcore.extension.callbacks;

import org.qubership.cloud.junit.cloudcore.extension.callbacks.classes.TestClass;
import org.qubership.cloud.junit.cloudcore.extension.callbacks.classes.TestClassLev2;
import org.qubership.cloud.junit.cloudcore.extension.provider.CloudCoreResourceFactory;
import org.qubership.cloud.junit.cloudcore.extension.provider.CloudCoreResourceFactoryProvider;
import org.qubership.cloud.junit.cloudcore.extension.provider.PortForwardConfig;
import org.qubership.cloud.junit.cloudcore.extension.service.Endpoint;
import org.qubership.cloud.junit.cloudcore.extension.service.NetSocketAddress;
import org.qubership.cloud.junit.cloudcore.extension.service.PortForwardParams;
import org.qubership.cloud.junit.cloudcore.extension.service.PortForwardService;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.net.URI;

import static org.mockito.Mockito.never;

public class CloudCoreJunitCallbackTest {
    String testCloud = "test-cloud";
    String testNamespace = "test-namespace";

    @Test
    void testBeforeAllTestKubernetesClient() throws Exception {

        System.setProperty("clouds.cloud_1.name", "test-cloud-1");
        System.setProperty("clouds.cloud_2.name", "test-cloud-2");
        System.setProperty("clouds.cloud_1.namespaces.origin", testNamespace);
        System.setProperty("clouds.cloud_2.namespaces.origin", testNamespace);

        ExtensionContext extensionContext = Mockito.mock(ExtensionContext.class);
        CloudCoreResourceFactory resourceFactory = Mockito.mock(CloudCoreResourceFactory.class);
        try (MockedStatic<CloudCoreResourceFactoryProvider> cloudCoreResourceFactoryProvider =
                     Mockito.mockStatic(CloudCoreResourceFactoryProvider.class)) {
            cloudCoreResourceFactoryProvider.when(() -> CloudCoreResourceFactoryProvider.getFactory(extensionContext))
                    .thenReturn(resourceFactory);
            KubernetesClient kubernetesClient_1 = Mockito.mock(KubernetesClient.class);
            KubernetesClient kubernetesClient_2 = Mockito.mock(KubernetesClient.class);
            PortForwardService portForwardService = Mockito.mock(PortForwardService.class);
            TestClass.TestKubernetesClient testInstance = new TestClass.TestKubernetesClient();
            Mockito.when(extensionContext.getRequiredTestInstance()).thenReturn(testInstance);
            PortForwardConfig hostnamePortForwardConfig = new PortForwardConfig(testCloud, testNamespace);
            Mockito.when(resourceFactory.getPortForwardService(hostnamePortForwardConfig)).thenReturn(portForwardService);
            Mockito.when(resourceFactory.getKubernetesClient("test-cloud-1", testNamespace)).thenReturn(kubernetesClient_1);
            Mockito.when(resourceFactory.getKubernetesClient("test-cloud-2", testNamespace)).thenReturn(kubernetesClient_2);

            new CloudCoreJunitCallback().beforeAll(extensionContext);

            Assertions.assertEquals(kubernetesClient_1, testInstance.getCloud1KubernetesClient());
            Assertions.assertEquals(kubernetesClient_2, testInstance.getCloud2KubernetesClient());
        }
    }

    @Test
    void testBeforeAllTestURI() throws Exception {

        System.setProperty("clouds.cloud.name", testCloud);
        System.setProperty("clouds.cloud.namespaces.namespace", testNamespace);

        ExtensionContext extensionContext = Mockito.mock(ExtensionContext.class);
        CloudCoreResourceFactory resourceFactory = Mockito.mock(CloudCoreResourceFactory.class);
        try (MockedStatic<CloudCoreResourceFactoryProvider> cloudCoreResourceFactoryProvider =
                     Mockito.mockStatic(CloudCoreResourceFactoryProvider.class)) {
            cloudCoreResourceFactoryProvider.when(() -> CloudCoreResourceFactoryProvider.getFactory(extensionContext))
                    .thenReturn(resourceFactory);
            KubernetesClient kubernetesClient = Mockito.mock(KubernetesClient.class);
            PortForwardService portForwardService = Mockito.mock(PortForwardService.class);
            TestClass.TestUri testInstance = new TestClass.TestUri();
            Mockito.when(extensionContext.getRequiredTestInstance()).thenReturn(testInstance);
            PortForwardConfig portForwardConfig = new PortForwardConfig(testCloud, testNamespace);
            Mockito.when(resourceFactory.getPortForwardService(portForwardConfig)).thenReturn(portForwardService);
            Mockito.when(resourceFactory.getKubernetesClient(testCloud, testNamespace)).thenReturn(kubernetesClient);
            Mockito.when(portForwardService.portForward(Mockito.any())).thenReturn(new NetSocketAddress("test-host", 8181));

            new CloudCoreJunitCallback().beforeAll(extensionContext);

            Assertions.assertEquals("ftp://test-host:8181/", testInstance.getUri().toString());
        }
    }

    @Test
    void testBeforeAllTestURL() throws Exception {

        System.setProperty("clouds.cloud.name", testCloud);
        System.setProperty("clouds.cloud.namespaces.namespace", testNamespace);

        ExtensionContext extensionContext = Mockito.mock(ExtensionContext.class);
        CloudCoreResourceFactory resourceFactory = Mockito.mock(CloudCoreResourceFactory.class);
        try (MockedStatic<CloudCoreResourceFactoryProvider> cloudCoreResourceFactoryProvider =
                     Mockito.mockStatic(CloudCoreResourceFactoryProvider.class)) {
            cloudCoreResourceFactoryProvider.when(() -> CloudCoreResourceFactoryProvider.getFactory(extensionContext))
                    .thenReturn(resourceFactory);
            KubernetesClient kubernetesClient = Mockito.mock(KubernetesClient.class);
            PortForwardService portForwardService = Mockito.mock(PortForwardService.class);
            TestClass.TestUrl testInstance = new TestClass.TestUrl();
            Mockito.when(extensionContext.getRequiredTestInstance()).thenReturn(testInstance);
            PortForwardConfig portForwardConfig = new PortForwardConfig(testCloud, testNamespace);
            Mockito.when(resourceFactory.getPortForwardService(portForwardConfig)).thenReturn(portForwardService);
            Mockito.when(resourceFactory.getKubernetesClient(testCloud, testNamespace)).thenReturn(kubernetesClient);
            Mockito.when(portForwardService.portForward(Mockito.any())).thenReturn(new NetSocketAddress("test-host", 8181));

            new CloudCoreJunitCallback().beforeAll(extensionContext);

            Assertions.assertEquals("https://test-host:8181/", testInstance.getUrl().toString());
        }
    }

    @Test
    void testBeforeAllTestTestPortForwardService() throws Exception {

        System.setProperty("clouds.cloud.name", testCloud);
        System.setProperty("clouds.cloud.namespaces.namespace", testNamespace);

        ExtensionContext extensionContext = Mockito.mock(ExtensionContext.class);
        CloudCoreResourceFactory resourceFactory = Mockito.mock(CloudCoreResourceFactory.class);
        try (MockedStatic<CloudCoreResourceFactoryProvider> cloudCoreResourceFactoryProvider =
                     Mockito.mockStatic(CloudCoreResourceFactoryProvider.class)) {
            cloudCoreResourceFactoryProvider.when(() -> CloudCoreResourceFactoryProvider.getFactory(extensionContext))
                    .thenReturn(resourceFactory);
            KubernetesClient kubernetesClient = Mockito.mock(KubernetesClient.class);
            PortForwardService portForwardService = Mockito.mock(PortForwardService.class);
            TestClass.TestPortForwardService testInstance = new TestClass.TestPortForwardService();
            Mockito.when(extensionContext.getRequiredTestInstance()).thenReturn(testInstance);
            PortForwardConfig portForwardConfig = new PortForwardConfig(testCloud, testNamespace);
            Mockito.when(resourceFactory.getPortForwardService(portForwardConfig)).thenReturn(portForwardService);
            Mockito.when(resourceFactory.getKubernetesClient(testCloud, testNamespace)).thenReturn(kubernetesClient);
            Mockito.when(portForwardService.portForward(Mockito.any())).thenReturn(new NetSocketAddress("test-host", 8181));

            new CloudCoreJunitCallback().beforeAll(extensionContext);

            Assertions.assertNotNull(testInstance.getPortForwardService());
        }
    }

    @Test
    void testBeforeAllTestNetSocketAddress() throws Exception {

        System.setProperty("clouds.cloud_1.name", testCloud);
        System.setProperty("clouds.cloud_1.namespaces.origin", testNamespace);

        ExtensionContext extensionContext = Mockito.mock(ExtensionContext.class);
        CloudCoreResourceFactory resourceFactory = Mockito.mock(CloudCoreResourceFactory.class);
        try (MockedStatic<CloudCoreResourceFactoryProvider> cloudCoreResourceFactoryProvider =
                     Mockito.mockStatic(CloudCoreResourceFactoryProvider.class)) {
            cloudCoreResourceFactoryProvider.when(() -> CloudCoreResourceFactoryProvider.getFactory(extensionContext))
                    .thenReturn(resourceFactory);
            KubernetesClient kubernetesClient = Mockito.mock(KubernetesClient.class);
            PortForwardService portForwardService = Mockito.mock(PortForwardService.class);
            TestClass.TestSocketAddress testInstance = new TestClass.TestSocketAddress();
            Mockito.when(extensionContext.getRequiredTestInstance()).thenReturn(testInstance);
            PortForwardConfig hostnamePortForwardConfig = new PortForwardConfig(testCloud, testNamespace);
            Mockito.when(resourceFactory.getPortForwardService(hostnamePortForwardConfig)).thenReturn(portForwardService);
            Mockito.when(resourceFactory.getKubernetesClient(testCloud, testNamespace)).thenReturn(kubernetesClient);
            Mockito.when(portForwardService.portForward(Mockito.any())).thenAnswer(i -> {
                PortForwardParams params = i.getArgument(0);
                return new NetSocketAddress(params.getServiceName(), params.getPort());
            });

            new CloudCoreJunitCallback().beforeAll(extensionContext);

            NetSocketAddress address = testInstance.getAddress();
            Assertions.assertEquals("postgres:5432", address.getEndpoint());
        }
    }

//    @Test
//    void testBeforeAllTest4() throws Exception {
//
//        System.setProperty("clouds.cloud.name", testCloud);
//        System.setProperty("clouds.cloud.namespaces.namespace", testNamespace);
//
//        ExtensionContext extensionContext = Mockito.mock(ExtensionContext.class);
//        CloudCoreResourceFactory resourceFactory = Mockito.mock(CloudCoreResourceFactory.class);
//        try (MockedStatic<CloudCoreResourceFactoryProvider> cloudCoreResourceFactoryProvider =
//                     Mockito.mockStatic(CloudCoreResourceFactoryProvider.class)) {
//            cloudCoreResourceFactoryProvider.when(() -> CloudCoreResourceFactoryProvider.getFactory(extensionContext))
//                    .thenReturn(resourceFactory);
//            KubernetesClient kubernetesClient = Mockito.mock(KubernetesClient.class);
//            PortForwardService portForwardService = Mockito.mock(PortForwardService.class);
//            TestClass.TestTokenService testInstance = new TestClass.TestTokenService();
//            Mockito.when(extensionContext.getRequiredTestInstance()).thenReturn(testInstance);
//            PortForwardConfig hostnamePortForwardConfig = new PortForwardConfig(testCloud, testNamespace);
//            Mockito.when(resourceFactory.getPortForwardService(hostnamePortForwardConfig)).thenReturn(portForwardService);
//            Mockito.when(resourceFactory.getKubernetesClient(testCloud, testNamespace)).thenReturn(kubernetesClient);
//            ArgumentCaptor<URL> urlArgumentCaptor = ArgumentCaptor.forClass(URL.class);
//            TokenService tokenServiceMock = Mockito.mock(TokenService.class);
//            Mockito.when(portForwardService.portForward(Mockito.any())).thenAnswer(i -> {
//                PortForwardParams params = i.getArgument(0);
//                return new NetSocketAddress(params.getServiceName(), params.getPort());
//            });
//            Mockito.when(tokenServiceMock.getGatewayUrl()).thenAnswer(i-> urlArgumentCaptor.getValue());
//
//            new CloudCoreJunitCallback().beforeAll(extensionContext);
//
//            TokenService tokenService = testInstance.getTokenService();
//            Assertions.assertNotNull(tokenService);
//            Assertions.assertEquals("http://internal-gateway-service:8080/", tokenService.getGatewayUrl().toString());
//        }
//    }

    @Test
    void testAfterAll() throws Exception {
        System.setProperty("clouds.cloud.name", testCloud);
        System.setProperty("clouds.cloud.namespaces.namespace", testNamespace);

        ExtensionContext extensionContext = Mockito.mock(ExtensionContext.class);
        CloudCoreResourceFactory resourceFactory = Mockito.mock(CloudCoreResourceFactory.class);
        try (MockedStatic<CloudCoreResourceFactoryProvider> cloudCoreResourceFactoryProvider =
                     Mockito.mockStatic(CloudCoreResourceFactoryProvider.class)) {
            cloudCoreResourceFactoryProvider.when(() -> CloudCoreResourceFactoryProvider.getFactory(extensionContext))
                    .thenReturn(resourceFactory);
            URI url1 = URI.create("http://test-1:8080/");
            URI url2 = URI.create("http://test-2:8080/");
            Object testInstance = new TestClassLev2(url1, url2);
            Mockito.when(extensionContext.getRequiredTestInstance()).thenReturn(testInstance);
            PortForwardService portForwardService = Mockito.mock(PortForwardService.class);
            PortForwardConfig portForwardConfig = new PortForwardConfig(testCloud, testNamespace);
            Mockito.when(resourceFactory.getPortForwardService(portForwardConfig)).thenReturn(portForwardService);

            new CloudCoreJunitCallback().afterAll(extensionContext);

            Mockito.verify(portForwardService).closePortForward(Mockito.eq(new Endpoint(url1.getHost(), url1.getPort())));
            Mockito.verify(portForwardService).closePortForward(Mockito.eq(new Endpoint(url2.getHost(), url2.getPort())));
            Mockito.verify(resourceFactory).close();
        }
    }

    @Test
    void testAfterAllAsEnclosingClass() throws Exception {
        System.setProperty("clouds.cloud.name", testCloud);
        System.setProperty("clouds.cloud.namespaces.namespace", testNamespace);

        ExtensionContext extensionContext = Mockito.mock(ExtensionContext.class);
        CloudCoreResourceFactory resourceFactory = Mockito.mock(CloudCoreResourceFactory.class);
        try (MockedStatic<CloudCoreResourceFactoryProvider> cloudCoreResourceFactoryProvider =
                     Mockito.mockStatic(CloudCoreResourceFactoryProvider.class)) {
            cloudCoreResourceFactoryProvider.when(() -> CloudCoreResourceFactoryProvider.getFactory(extensionContext)).thenReturn(resourceFactory);
            URI url = Mockito.mock(URI.class);
            Object testInstance = new TestClassLev2.TestInnerClass(url);
            Mockito.when(extensionContext.getRequiredTestInstance()).thenReturn(testInstance);
            PortForwardService portForwardService = Mockito.mock(PortForwardService.class);
            PortForwardConfig portForwardConfig = new PortForwardConfig(testCloud, testNamespace);
            Mockito.when(resourceFactory.getPortForwardService(portForwardConfig)).thenReturn(portForwardService);

            new CloudCoreJunitCallback().afterAll(extensionContext);

            Mockito.verify(portForwardService).closePortForward(new Endpoint(url.getHost(), url.getPort()));
            Mockito.verify(resourceFactory, never()).close();
        }
    }
}
