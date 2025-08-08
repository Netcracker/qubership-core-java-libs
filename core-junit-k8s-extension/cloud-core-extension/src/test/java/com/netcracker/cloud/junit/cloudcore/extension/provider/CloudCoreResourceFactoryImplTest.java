package com.netcracker.cloud.junit.cloudcore.extension.provider;

import org.qubership.cloud.junit.cloudcore.extension.client.KubernetesClientFactory;
import org.qubership.cloud.junit.cloudcore.extension.service.PortForwardService;
import io.fabric8.kubernetes.api.model.NamedContext;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CloudCoreResourceFactoryImplTest {

    @Test
    void testGetKubernetesClientCached() {
        String cloud1 = "test-cloud-1";
        String cloud2 = "test-cloud-2";
        String namespace1 = "test-ns-1";
        String namespace2 = "test-ns-2";
        Config config = Mockito.mock(Config.class);
        NamedContext ctx1 = Mockito.mock(NamedContext.class);
        NamedContext ctx2 = Mockito.mock(NamedContext.class);

        Mockito.when(ctx1.getName()).thenReturn(cloud1);
        Mockito.when(ctx2.getName()).thenReturn(cloud2);

        KubernetesClientFactory kubernetesClientFactory = new KubernetesClientFactory(config);
        Mockito.when(config.getCurrentContext()).thenReturn(ctx1, ctx2);
        Mockito.when(config.getContexts()).thenReturn(List.of(ctx1, ctx2));

        CloudCoreResourceFactoryImpl factory = new CloudCoreResourceFactoryImpl(kubernetesClientFactory);
        KubernetesClient client1_1_1 = factory.getKubernetesClient(cloud1, namespace1);
        KubernetesClient client1_1_2 = factory.getKubernetesClient(cloud1, namespace1);
        KubernetesClient client1_2 = factory.getKubernetesClient(cloud1, namespace2);
        KubernetesClient client2_1 = factory.getKubernetesClient(cloud2, namespace1);
        KubernetesClient client2_2_1 = factory.getKubernetesClient(cloud2, namespace2);
        KubernetesClient client2_2_2 = factory.getKubernetesClient(cloud2, namespace2);
        Assertions.assertSame(client1_1_1, client1_1_2);
        Assertions.assertNotSame(client1_1_1, client1_2);
        Assertions.assertNotSame(client2_1, client2_2_1);
        Assertions.assertSame(client2_2_1, client2_2_2);
    }

    @Test
    void testGetFactory_shouldReturnHostNamePortForward() {
        KubernetesClientFactory kubernetesClientFactory = Mockito.mock(KubernetesClientFactory.class);
        KubernetesClient kubernetesClient = Mockito.mock(KubernetesClient.class);
        Mockito.when(kubernetesClientFactory.getKubernetesClient(Mockito.any(), Mockito.any())).thenReturn(kubernetesClient);
        CloudCoreResourceFactoryImpl factory = new CloudCoreResourceFactoryImpl(kubernetesClientFactory);
        PortForwardConfig portForwardConfig = new PortForwardConfig("test-cloud", "test-namespace");
        Class<?> portForwardServiceClass = factory.getPortForwardService(portForwardConfig).getClass();
        assertEquals(PortForwardService.class, portForwardServiceClass);
    }
}
