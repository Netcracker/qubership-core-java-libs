package com.netcracker.cloud.junit.cloudcore.extension.client;

import io.fabric8.kubernetes.api.model.AuthProviderConfig;
import io.fabric8.kubernetes.api.model.NamedContext;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.http.HttpClient;
import io.fabric8.kubernetes.client.utils.HttpClientUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class KubernetesClientFactoryTest {
    String cloud = "test-cloud";
    String namespace = "test-namespace";


    @Test
    void testWithHttpClientBuilderConsumer() {
        System.setProperty("clouds.cloud.name", cloud);
        System.setProperty("clouds.cloud.namespaces.namespace", namespace);

        Config config = Mockito.mock(Config.class);
        AtomicReference<Consumer<HttpClient.Builder>> consumer = new AtomicReference<>();
        KubernetesClient expectedClient = Mockito.mock(KubernetesClient.class);
        HttpClient.Builder httpClientBuilder = Mockito.mock(HttpClient.Builder.class);

        try (MockedConstruction<ConfigBuilder> configBuilderConstructor = Mockito.mockConstruction(ConfigBuilder.class,
                (configBuilder, context) -> {
                    Mockito.when(configBuilder.withNamespace(namespace)).thenReturn(configBuilder);
                    Mockito.when(configBuilder.withTrustCerts(true)).thenReturn(configBuilder);
                    Mockito.when(configBuilder.withDisableHostnameVerification(true)).thenReturn(configBuilder);
                    Mockito.when(configBuilder.withRequestRetryBackoffLimit(3)).thenReturn(configBuilder);
                    Mockito.when(configBuilder.withWatchReconnectLimit(3)).thenReturn(configBuilder);
                    Mockito.when(configBuilder.withRequestTimeout(5000)).thenReturn(configBuilder);
                    Mockito.when(configBuilder.withWebsocketPingInterval(10000)).thenReturn(configBuilder);
                    Mockito.when(configBuilder.withWatchReconnectInterval(5000)).thenReturn(configBuilder);
                    Mockito.when(configBuilder.build()).thenReturn(config);
                }); MockedConstruction<KubernetesClientBuilder> kubernetesClientBuilderMock = Mockito.mockConstruction(KubernetesClientBuilder.class,
                (kubernetesClientBuilder, context) -> {
                    Mockito.when(kubernetesClientBuilder.withConfig(Mockito.any(Config.class))).thenReturn(kubernetesClientBuilder);
                    Mockito.when(kubernetesClientBuilder.withHttpClientFactory(Mockito.any())).thenReturn(kubernetesClientBuilder);
                    Mockito.when(kubernetesClientBuilder.withHttpClientBuilderConsumer(Mockito.any())).thenAnswer(i -> {
                        consumer.set(i.getArgument(0));
                        return kubernetesClientBuilder;
                    });
                    Mockito.when(kubernetesClientBuilder.build()).thenAnswer(i -> {
                        Consumer<HttpClient.Builder> c = consumer.get();
                        c.accept(httpClientBuilder);
                        return expectedClient;
                    });

                });
             MockedStatic<HttpClientUtils> httpClientUtilsStatic = Mockito.mockStatic(HttpClientUtils.class)) {
            HttpClient.Factory factory = Mockito.mock(HttpClient.Factory.class);
            httpClientUtilsStatic.when(HttpClientUtils::getHttpClientFactory).thenReturn(factory);
            Mockito.when(factory.newBuilder(config)).thenReturn(httpClientBuilder);
            HttpClient httpClient = Mockito.mock(HttpClient.class);
            Mockito.when(httpClientBuilder.build()).thenReturn(httpClient);
            AuthProviderConfig authProviderConfig = Mockito.mock(AuthProviderConfig.class);
            NamedContext context = Mockito.mock(NamedContext.class);
            Mockito.when(context.getName()).thenReturn(cloud);
            Mockito.when(config.getContexts()).thenReturn(List.of(context));
            Mockito.when(config.getCurrentContext()).thenReturn(context);
            Mockito.when(config.getAuthProvider()).thenReturn(authProviderConfig);
            Mockito.when(authProviderConfig.getName()).thenReturn("oidc");
            Mockito.when(authProviderConfig.getConfig()).thenReturn(Map.of("refresh-token", "test"));
            DefaultKubernetesClientFactory kubernetesClientFactory = new DefaultKubernetesClientFactory(config);
            KubernetesClient kubernetesClient = kubernetesClientFactory.getKubernetesClient(cloud, namespace);
            Assertions.assertNotNull(kubernetesClient);
            Mockito.verify(httpClientBuilder).connectTimeout(15, TimeUnit.SECONDS);
        }
    }
}
