package com.netcracker.cloud.junit.cloudcore.extension.service;

import com.netcracker.cloud.junit.cloudcore.extension.provider.LocalHostAddressGenerator;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.LocalPortForward;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.ServiceResource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.InetAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PortForwardServiceTest {
    private static final String SERVICE_NAME = "test-service";
    private static final String NAMESPACE = "test-namespace";

    @Test
    void testPortForward() throws Exception {
        String host = "test-service.test-namespace.svc.test.cloud.com";
        try (MockedStatic<LocalHostAddressGenerator> localHostAddressGeneratorMockedStatic =
                     Mockito.mockStatic(LocalHostAddressGenerator.class)) {

            KubernetesClient kubernetesClient = mock(KubernetesClient.class);
            when(kubernetesClient.getNamespace()).thenReturn(NAMESPACE);
            when(kubernetesClient.getMasterUrl()).thenReturn(URI.create("http://test.cloud.com").toURL());

            InetAddress inetAddress = mock(InetAddress.class);
            when(inetAddress.isReachable(Mockito.anyInt())).thenReturn(true);
            localHostAddressGeneratorMockedStatic.when(() -> LocalHostAddressGenerator.getOrNext(Mockito.eq(host))).thenReturn(inetAddress);

            MixedOperation<Service, ServiceList, ServiceResource<Service>> testMixedOperation = mock(TestMixedOperation.class);
            when(kubernetesClient.services()).thenReturn(testMixedOperation);
            NonNamespaceOperation<Service, ServiceList, ServiceResource<Service>> testNonNamespaceOperation = mock(TestNonNamespaceOperation.class);
            when(testMixedOperation.inNamespace(any())).thenReturn(testNonNamespaceOperation);
            ServiceResource<Service> testServiceResource = mock(TestServiceResource.class);
            when(testNonNamespaceOperation.withName(SERVICE_NAME)).thenReturn(testServiceResource);
            LocalPortForward localPortForward8080 = mock(LocalPortForward.class);
            LocalPortForward localPortForward8181 = mock(LocalPortForward.class);
            when(testServiceResource.portForward(Mockito.anyInt(), Mockito.any(InetAddress.class), Mockito.anyInt())).then(i -> {
                int localPort = i.getArgument(2);
                LocalPortForward localPortForward;
                if (localPort == 8080) localPortForward = localPortForward8080;
                else localPortForward = localPortForward8181;
                when(localPortForward.getLocalAddress()).thenReturn(i.getArgument(1));
                when(localPortForward.getLocalPort()).thenReturn(localPort);
                return localPortForward;
            });

            Map<Endpoint, LocalPortForward> cache = new HashMap<>();
            PortForwardService portForwardService = new PortForwardService(kubernetesClient, cache);

            NetSocketAddress netSocketAddress1_attempt1 = portForwardService.portForward(new PortForwardParams(SERVICE_NAME, 8080));
            assertNotNull(netSocketAddress1_attempt1);

            NetSocketAddress netSocketAddress1_attempt2 = portForwardService.portForward(new PortForwardParams(SERVICE_NAME, 8080));
            assertNotNull(netSocketAddress1_attempt2);
            assertEquals(netSocketAddress1_attempt1, netSocketAddress1_attempt2);

            NetSocketAddress netSocketAddress2 = portForwardService.portForward(new PortForwardParams(SERVICE_NAME, 8181));
            assertNotNull(netSocketAddress2);
            assertNotEquals(netSocketAddress1_attempt1, netSocketAddress2);

            portForwardService.closePortForward(new Endpoint(host, 8080));
            assertEquals(1, cache.size());
            verify(localPortForward8080).close();
            verify(localPortForward8181, times(0)).close();

            ArgumentCaptor<Supplier<Boolean>> supplierArgumentCaptor = ArgumentCaptor.forClass(Supplier.class);
            localHostAddressGeneratorMockedStatic.verify(() -> LocalHostAddressGenerator.cleanup(Mockito.eq(host), supplierArgumentCaptor.capture()));

            assertEquals(false, supplierArgumentCaptor.getValue().get());

            portForwardService.closePortForwards();
            assertEquals(0, cache.size());
            verify(localPortForward8181).close();

            assertEquals(true, supplierArgumentCaptor.getValue().get());
        }
    }

    @Test
    void testPortForwardAlreadyBoundIp() throws Exception {
        String host = "test-service.test-namespace.svc.test.cloud.com";
        try (MockedStatic<LocalHostAddressGenerator> localHostAddressGeneratorMockedStatic =
                     Mockito.mockStatic(LocalHostAddressGenerator.class)) {

            KubernetesClient kubernetesClient = mock(KubernetesClient.class);
            when(kubernetesClient.getNamespace()).thenReturn(NAMESPACE);
            when(kubernetesClient.getMasterUrl()).thenReturn(URI.create("http://test.cloud.com").toURL());

            InetAddress inetAddress = mock(InetAddress.class);
            when(inetAddress.isReachable(Mockito.anyInt())).thenReturn(true);
            localHostAddressGeneratorMockedStatic.when(() -> LocalHostAddressGenerator.getOrNext(Mockito.eq(host))).thenReturn(inetAddress);

            MixedOperation<Service, ServiceList, ServiceResource<Service>> testMixedOperation = mock(TestMixedOperation.class);
            when(kubernetesClient.services()).thenReturn(testMixedOperation);
            NonNamespaceOperation<Service, ServiceList, ServiceResource<Service>> testNonNamespaceOperation = mock(TestNonNamespaceOperation.class);
            when(testMixedOperation.inNamespace(any())).thenReturn(testNonNamespaceOperation);
            ServiceResource<Service> testServiceResource = mock(TestServiceResource.class);
            when(testNonNamespaceOperation.withName(SERVICE_NAME)).thenReturn(testServiceResource);
            LocalPortForward localPortForward8080 = mock(LocalPortForward.class);
            LocalPortForward localPortForward8181 = mock(LocalPortForward.class);
            when(testServiceResource.portForward(Mockito.anyInt(), Mockito.any(InetAddress.class), Mockito.anyInt())).then(i -> {
                int localPort = i.getArgument(2);
                LocalPortForward localPortForward;
                if (localPort == 8080) localPortForward = localPortForward8080;
                else localPortForward = localPortForward8181;
                when(localPortForward.getLocalAddress()).thenReturn(i.getArgument(1));
                when(localPortForward.getLocalPort()).thenReturn(localPort);
                return localPortForward;
            });

            Map<Endpoint, LocalPortForward> cache = new HashMap<>();
            PortForwardService portForwardService = new PortForwardService(kubernetesClient, cache);

            NetSocketAddress netSocketAddress1_attempt1 = portForwardService.portForward(new PortForwardParams(SERVICE_NAME, 8080));
            assertNotNull(netSocketAddress1_attempt1);

            NetSocketAddress netSocketAddress1_attempt2 = portForwardService.portForward(new PortForwardParams(SERVICE_NAME, 8080));
            assertNotNull(netSocketAddress1_attempt2);
            assertEquals(netSocketAddress1_attempt1, netSocketAddress1_attempt2);

            NetSocketAddress netSocketAddress2 = portForwardService.portForward(new PortForwardParams(SERVICE_NAME, 8181));
            assertNotNull(netSocketAddress2);
            assertNotEquals(netSocketAddress1_attempt1, netSocketAddress2);

            portForwardService.closePortForward(new Endpoint(host, 8080));
            assertEquals(1, cache.size());
            verify(localPortForward8080).close();
            verify(localPortForward8181, times(0)).close();

            ArgumentCaptor<Supplier<Boolean>> supplierArgumentCaptor = ArgumentCaptor.forClass(Supplier.class);
            localHostAddressGeneratorMockedStatic.verify(() -> LocalHostAddressGenerator.cleanup(Mockito.eq(host), supplierArgumentCaptor.capture()));

            assertEquals(false, supplierArgumentCaptor.getValue().get());

            portForwardService.closePortForwards();
            assertEquals(0, cache.size());
            verify(localPortForward8181).close();

            assertEquals(true, supplierArgumentCaptor.getValue().get());
        }
    }

    private interface TestMixedOperation extends MixedOperation<Service, ServiceList, ServiceResource<Service>> {
    }

    private interface TestNonNamespaceOperation extends NonNamespaceOperation<Service, ServiceList, ServiceResource<Service>> {
    }

    private interface TestServiceResource extends ServiceResource<Service> {
    }
}
