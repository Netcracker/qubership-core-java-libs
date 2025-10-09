package com.netcracker.cloud.junit.cloudcore.extension.provider;

import com.netcracker.cloud.junit.cloudcore.extension.client.DefaultKubernetesClientFactory;
import com.netcracker.cloud.junit.cloudcore.extension.client.TestKubernetesClientFactory;
import com.netcracker.cloud.junit.cloudcore.extension.service.PortForwardService;
import io.fabric8.kubernetes.api.model.NamedContext;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import java.util.List;

import static com.netcracker.cloud.junit.cloudcore.extension.provider.DefaultPortForwardServiceManager.PORTFORWARD_FQDN_ENABLED_PROP;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        Mockito.when(config.getCurrentContext()).thenReturn(ctx1, ctx2);
        Mockito.when(config.getContexts()).thenReturn(List.of(ctx1, ctx2));

        try (MockedConstruction<KubernetesClientBuilder> kubernetesClientBuilderMock = Mockito.mockConstruction(KubernetesClientBuilder.class, (mock, context) -> {
            KubernetesClientBuilder kubernetesClientBuilder = Mockito.mock(KubernetesClientBuilder.class);
            KubernetesClient kubernetesClient = Mockito.mock(KubernetesClient.class);
            Mockito.when(mock.withConfig(Mockito.any(Config.class))).thenReturn(kubernetesClientBuilder);
            Mockito.when(kubernetesClientBuilder.withHttpClientFactory(Mockito.any())).thenReturn(kubernetesClientBuilder);
            Mockito.when(kubernetesClientBuilder.withHttpClientBuilderConsumer(Mockito.any())).thenReturn(kubernetesClientBuilder);
            Mockito.when(kubernetesClientBuilder.build()).thenReturn(kubernetesClient);
        });
             DefaultKubernetesClientFactory factory = new DefaultKubernetesClientFactory(config)) {
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
    }

    @Test
    void testCreatePortForwardNoFQDN() {
        try (DefaultPortForwardServiceManager factory = new DefaultPortForwardServiceManager()) {
            System.setProperty("clouds.test-cloud1.name", "test-cloud-1");
            System.setProperty("clouds.test-cloud1.namespaces.test-namespace", "test-namespace");

            DefaultKubernetesClientFactory kubernetesClientFactory = Mockito.mock(DefaultKubernetesClientFactory.class);
            KubernetesClient kubernetesClient = Mockito.mock(KubernetesClient.class);
            Mockito.when(kubernetesClientFactory.getKubernetesClient(Mockito.any(), Mockito.any())).thenReturn(kubernetesClient);
            TestKubernetesClientFactory.setFunction(cloudAndNamespace -> kubernetesClient);
            PortForwardConfig portForwardConfig = new PortForwardConfig("test-cloud-1", "test-namespace");
            PortForwardService portForwardService = factory.getPortForwardService(portForwardConfig);
            assertFalse(portForwardService.isFqdn());
        } finally {
            System.clearProperty("clouds.test-cloud1.name");
            System.clearProperty("clouds.test-cloud1.namespaces.test-namespace");
        }
    }

    @Test
    void testCreatePortForwardOverrideFQDN() {
        try (DefaultPortForwardServiceManager factory = new DefaultPortForwardServiceManager()) {
            System.setProperty("clouds.test-cloud1.name", "test-cloud-1");
            System.setProperty("clouds.test-cloud1.namespaces.test-namespace", "test-namespace");
            System.setProperty(PORTFORWARD_FQDN_ENABLED_PROP, "true");

            DefaultKubernetesClientFactory kubernetesClientFactory = Mockito.mock(DefaultKubernetesClientFactory.class);
            KubernetesClient kubernetesClient = Mockito.mock(KubernetesClient.class);
            Mockito.when(kubernetesClientFactory.getKubernetesClient(Mockito.any(), Mockito.any())).thenReturn(kubernetesClient);
            TestKubernetesClientFactory.setFunction(cloudAndNamespace -> kubernetesClient);
            PortForwardConfig portForwardConfig = new PortForwardConfig("test-cloud-1", "test-namespace");
            PortForwardService portForwardService = factory.getPortForwardService(portForwardConfig);
            assertTrue(portForwardService.isFqdn());
        } finally {
            System.clearProperty("clouds.test-cloud1.name");
            System.clearProperty("clouds.test-cloud1.namespaces.test-namespace");
            System.clearProperty("portforward.fqdn.hosts.enabled");
        }
    }

    @Test
    void testCreatePortForwardFQDN() {
        try (DefaultPortForwardServiceManager factory = new DefaultPortForwardServiceManager()) {
            System.setProperty("clouds.test-cloud1.name", "test-cloud-1");
            System.setProperty("clouds.test-cloud2.name", "test-cloud-2");
            System.setProperty("clouds.test-cloud1.namespaces.test-namespace", "test-namespace");
            System.setProperty("clouds.test-cloud2.namespaces.test-namespace", "test-namespace");

            DefaultKubernetesClientFactory kubernetesClientFactory = Mockito.mock(DefaultKubernetesClientFactory.class);
            KubernetesClient kubernetesClient = Mockito.mock(KubernetesClient.class);
            Mockito.when(kubernetesClientFactory.getKubernetesClient(Mockito.any(), Mockito.any())).thenReturn(kubernetesClient);
            TestKubernetesClientFactory.setFunction(cloudAndNamespace -> kubernetesClient);
            PortForwardConfig portForwardConfig1 = new PortForwardConfig("test-cloud-1", "test-namespace");
            PortForwardService portForwardService1 = factory.getPortForwardService(portForwardConfig1);
            PortForwardConfig portForwardConfig2 = new PortForwardConfig("test-cloud-2", "test-namespace");
            PortForwardService portForwardService2 = factory.getPortForwardService(portForwardConfig2);
            assertTrue(portForwardService1.isFqdn());
            assertTrue(portForwardService2.isFqdn());
        } finally {
            System.clearProperty("clouds.test-cloud1.name");
            System.clearProperty("clouds.test-cloud2.name");
            System.clearProperty("clouds.test-cloud1.namespaces.test-namespace");
            System.clearProperty("clouds.test-cloud2.namespaces.test-namespace");
        }
    }

}
