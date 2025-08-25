package com.netcracker.cloud.junit.cloudcore.extension.service;

import com.netcracker.cloud.junit.cloudcore.extension.provider.LocalHostAddressGenerator;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.LocalPortForward;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.PodResource;
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

            MixedOperation<Service, ServiceList, ServiceResource<Service>> testMixedOperation = mock(ServiceTestMixedOperation.class);
            when(kubernetesClient.services()).thenReturn(testMixedOperation);
            NonNamespaceOperation<Service, ServiceList, ServiceResource<Service>> testNonNamespaceOperation = mock(ServiceTestNonNamespaceOperation.class);
            when(testMixedOperation.inNamespace(any())).thenReturn(testNonNamespaceOperation);
            ServiceResource<Service> testServiceResource = mock(ServiceTestServiceResource.class);
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

            NetSocketAddress netSocketAddress1_attempt1 = portForwardService.portForward(ServicePortForwardParams.builder(SERVICE_NAME, 8080).build());
            assertNotNull(netSocketAddress1_attempt1);

            NetSocketAddress netSocketAddress1_attempt2 = portForwardService.portForward(ServicePortForwardParams.builder(SERVICE_NAME, 8080).build());
            assertNotNull(netSocketAddress1_attempt2);
            assertEquals(netSocketAddress1_attempt1, netSocketAddress1_attempt2);

            NetSocketAddress netSocketAddress2 = portForwardService.portForward(ServicePortForwardParams.builder(SERVICE_NAME, 8181).build());
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
    void testPortForwardWithPodName() throws Exception {
        String host_pod1 = "pod-1.test-namespace.svc.test.cloud.com";
        String host_pod2 = "pod-2.test-namespace.svc.test.cloud.com";
        try (MockedStatic<LocalHostAddressGenerator> localHostAddressGeneratorMockedStatic =
                     Mockito.mockStatic(LocalHostAddressGenerator.class)) {

            KubernetesClient kubernetesClient = mock(KubernetesClient.class);
            when(kubernetesClient.getNamespace()).thenReturn(NAMESPACE);
            when(kubernetesClient.getMasterUrl()).thenReturn(URI.create("http://test.cloud.com").toURL());

            InetAddress inetAddress1 = mock(InetAddress.class);
            when(inetAddress1.isReachable(Mockito.anyInt())).thenReturn(true);
            InetAddress inetAddress2 = mock(InetAddress.class);
            when(inetAddress2.isReachable(Mockito.anyInt())).thenReturn(true);
            localHostAddressGeneratorMockedStatic.when(() -> LocalHostAddressGenerator.getOrNext(Mockito.anyString())).thenAnswer(i -> {
                String host = i.getArgument(0);
                return host_pod1.equals(host) ? inetAddress1 : inetAddress2;
            });

            MixedOperation<Pod, PodList, PodResource> testMixedOperation = mock(PodTestMixedOperation.class);
            when(kubernetesClient.pods()).thenReturn(testMixedOperation);
            NonNamespaceOperation<Pod, PodList, PodResource> testNonNamespaceOperation = mock(PodTestNonNamespaceOperation.class);
            when(testMixedOperation.inNamespace(any())).thenReturn(testNonNamespaceOperation);
            PodResource testResource = mock(PodTestServiceResource.class);
            when(testNonNamespaceOperation.withName(Mockito.anyString())).thenReturn(testResource);
            LocalPortForward localPortForward8080_pod1 = mock(LocalPortForward.class);
            LocalPortForward localPortForward8080_pod2 = mock(LocalPortForward.class);
            when(testResource.portForward(Mockito.anyInt(), Mockito.any(InetAddress.class), Mockito.anyInt())).then(i -> {
                InetAddress inetAddress = i.getArgument(1);
                LocalPortForward localPortForward;
                if (inetAddress == inetAddress1) localPortForward = localPortForward8080_pod1;
                else localPortForward = localPortForward8080_pod2;
                when(localPortForward.getLocalAddress()).thenReturn(i.getArgument(1));
                when(localPortForward.getLocalPort()).thenReturn(i.getArgument(2));
                return localPortForward;
            });

            Map<Endpoint, LocalPortForward> cache = new HashMap<>();
            PortForwardService portForwardService = new PortForwardService(kubernetesClient, cache);

            NetSocketAddress netSocketAddress1_attempt1 = portForwardService.portForward(PodPortForwardParams.builder(SERVICE_NAME, 8080).podName("pod-1").build());
            assertNotNull(netSocketAddress1_attempt1);

            NetSocketAddress netSocketAddress1_attempt2 = portForwardService.portForward(PodPortForwardParams.builder(SERVICE_NAME, 8080).podName("pod-1").build());
            assertNotNull(netSocketAddress1_attempt2);
            assertEquals(netSocketAddress1_attempt1, netSocketAddress1_attempt2);

            NetSocketAddress netSocketAddress2 = portForwardService.portForward(PodPortForwardParams.builder(SERVICE_NAME, 8080).podName("pod-2").build());
            assertNotNull(netSocketAddress2);
            assertNotEquals(netSocketAddress1_attempt1, netSocketAddress2);

            assertEquals(2, cache.size());

            portForwardService.closePortForward(new Endpoint(host_pod1, 8080));
            portForwardService.closePortForward(new Endpoint(host_pod2, 8080));

            verify(localPortForward8080_pod1).close();
            verify(localPortForward8080_pod2).close();

            ArgumentCaptor<Supplier<Boolean>> supplierArgumentCaptor_pod1 = ArgumentCaptor.forClass(Supplier.class);
            ArgumentCaptor<Supplier<Boolean>> supplierArgumentCaptor_pod2 = ArgumentCaptor.forClass(Supplier.class);
            localHostAddressGeneratorMockedStatic.verify(() -> LocalHostAddressGenerator.cleanup(Mockito.eq(host_pod1), supplierArgumentCaptor_pod1.capture()));
            localHostAddressGeneratorMockedStatic.verify(() -> LocalHostAddressGenerator.cleanup(Mockito.eq(host_pod2), supplierArgumentCaptor_pod2.capture()));

            assertEquals(true, supplierArgumentCaptor_pod1.getValue().get());
            assertEquals(true, supplierArgumentCaptor_pod2.getValue().get());
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

            MixedOperation<Service, ServiceList, ServiceResource<Service>> testMixedOperation = mock(ServiceTestMixedOperation.class);
            when(kubernetesClient.services()).thenReturn(testMixedOperation);
            NonNamespaceOperation<Service, ServiceList, ServiceResource<Service>> testNonNamespaceOperation = mock(ServiceTestNonNamespaceOperation.class);
            when(testMixedOperation.inNamespace(any())).thenReturn(testNonNamespaceOperation);
            ServiceResource<Service> testServiceResource = mock(ServiceTestServiceResource.class);
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

            NetSocketAddress netSocketAddress1_attempt1 = portForwardService.portForward(ServicePortForwardParams.builder(SERVICE_NAME, 8080).build());
            assertNotNull(netSocketAddress1_attempt1);

            NetSocketAddress netSocketAddress1_attempt2 = portForwardService.portForward(ServicePortForwardParams.builder(SERVICE_NAME, 8080).build());
            assertNotNull(netSocketAddress1_attempt2);
            assertEquals(netSocketAddress1_attempt1, netSocketAddress1_attempt2);

            NetSocketAddress netSocketAddress2 = portForwardService.portForward(ServicePortForwardParams.builder(SERVICE_NAME, 8181).build());
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

    private interface ServiceTestMixedOperation extends MixedOperation<Service, ServiceList, ServiceResource<Service>> {
    }

    private interface ServiceTestNonNamespaceOperation extends NonNamespaceOperation<Service, ServiceList, ServiceResource<Service>> {
    }

    private interface ServiceTestServiceResource extends ServiceResource<Service> {
    }

    private interface PodTestMixedOperation extends MixedOperation<Pod, PodList, PodResource> {
    }

    private interface PodTestNonNamespaceOperation extends NonNamespaceOperation<Pod, PodList, PodResource> {
    }

    private interface PodTestServiceResource extends PodResource {
    }
}
