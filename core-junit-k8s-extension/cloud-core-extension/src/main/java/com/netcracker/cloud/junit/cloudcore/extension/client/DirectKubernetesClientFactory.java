package com.netcracker.cloud.junit.cloudcore.extension.client;

import com.netcracker.cloud.junit.cloudcore.extension.annotations.Conditional;
import com.netcracker.cloud.junit.cloudcore.extension.annotations.Priority;
import com.netcracker.cloud.junit.cloudcore.extension.provider.CloudAndNamespace;
import io.fabric8.kubernetes.api.model.NamedContext;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;

import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import static com.netcracker.cloud.junit.cloudcore.extension.provider.KubernetesClientProvider.IN_K8S;

@Priority
@Conditional(IN_K8S)
public class DirectKubernetesClientFactory implements AutoCloseable, KubernetesClientFactory {

    public static final String NAMESPACE = "NAMESPACE";

    private final Config config;

    public DirectKubernetesClientFactory() {
        this(new ConfigBuilder().build());
    }

    public DirectKubernetesClientFactory(Config config) {
        this.config = config;
    }

    private final static ConcurrentHashMap<CloudAndNamespace, KubernetesClient> clientsMap = new ConcurrentHashMap<>();

    public Collection<String> getKubernetesContexts() {
        return config.getContexts().stream().map(NamedContext::getName).toList();
    }

    public KubernetesClient getKubernetesClient(String context, String namespace) {
        return clientsMap.computeIfAbsent(new CloudAndNamespace(context, namespace), cloudAndNamespace -> {
            Config config = Config.autoConfigure(null);
            config.setNamespace(System.getenv(NAMESPACE));
            KubernetesClientBuilder kubernetesClientBuilder = new KubernetesClientBuilder().withConfig(config);
            return kubernetesClientBuilder.build();
        });
    }

    @Override
    public String getCurrentContext() {
        return config.getCurrentContext() != null ? config.getCurrentContext().getName() : "this";
    }

    @Override
    public String getNamespace() {
        return config.getNamespace() != null ? config.getNamespace() : NAMESPACE;
    }

    @Override
    public void close() {
        HashMap<CloudAndNamespace, KubernetesClient> copy = new HashMap<>(clientsMap);
        clientsMap.clear();
        copy.values().forEach(KubernetesClient::close);
    }
}
