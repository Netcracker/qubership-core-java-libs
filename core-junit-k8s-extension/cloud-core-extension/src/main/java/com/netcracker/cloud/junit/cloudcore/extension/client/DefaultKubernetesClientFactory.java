package com.netcracker.cloud.junit.cloudcore.extension.client;

import com.netcracker.cloud.junit.cloudcore.extension.provider.CloudAndNamespace;
import io.fabric8.kubernetes.api.model.NamedContext;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;

import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static io.fabric8.kubernetes.client.utils.HttpClientUtils.getHttpClientFactory;

public class DefaultKubernetesClientFactory implements AutoCloseable, KubernetesClientFactory {

    private final Config config;

    public DefaultKubernetesClientFactory() {
        this(new ConfigBuilder().build());
    }

    public DefaultKubernetesClientFactory(Config config) {
        this.config = config;
    }

    private final ConcurrentHashMap<CloudAndNamespace, KubernetesClient> clientsMap = new ConcurrentHashMap<>();

    public Collection<String> getKubernetesContexts() {
        return config.getContexts().stream().map(NamedContext::getName).toList();
    }

    public KubernetesClient getKubernetesClient(String context, String namespace) {
        return clientsMap.computeIfAbsent(new CloudAndNamespace(context, namespace), cloudAndNamespace -> {
            String cloud = cloudAndNamespace.getCloud();
            NamedContext namedContext = config.getContexts().stream()
                    .filter(c -> Objects.equals(c.getName(), cloud)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Unknown context: '%s'. Known contexts:\n[%s]",
                            cloud, String.join(",\n", getKubernetesContexts()))));
            Config config;
            if (Objects.equals(cloud, this.config.getCurrentContext().getName())) {
                config = this.config;
            } else {
                config = Config.autoConfigure(namedContext.getName());
            }
            config = new ConfigBuilder(config)
                    .withNamespace(cloudAndNamespace.getNamespace())
                    .withTrustCerts(true)
                    .withDisableHostnameVerification(true)
                    .withRequestRetryBackoffLimit(3)
                    .withWatchReconnectLimit(3)
                    .withRequestTimeout(5000)
                    .withWebsocketPingInterval(10000)
                    .withWatchReconnectInterval(5000)
                    .build();
            return new KubernetesClientBuilder().withConfig(config)
                    .withHttpClientFactory(getHttpClientFactory())
                    .withHttpClientBuilderConsumer(builder -> builder.connectTimeout(15, TimeUnit.SECONDS))
                    .build();
        });
    }

    @Override
    public void close() {
        HashMap<CloudAndNamespace, KubernetesClient> copy = new HashMap<>(clientsMap);
        clientsMap.clear();
        copy.values().forEach(KubernetesClient::close);
    }
}
