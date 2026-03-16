package com.netcracker.cloud.junit.cloudcore.extension.client;

import com.netcracker.cloud.junit.cloudcore.extension.annotations.Priority;
import com.netcracker.cloud.junit.cloudcore.extension.provider.CloudAndNamespace;
import com.netcracker.cloud.junit.cloudcore.extension.provider.OrderedServiceLoader;
import io.fabric8.kubernetes.api.model.NamedContext;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static com.netcracker.cloud.junit.cloudcore.extension.provider.OrderedServiceLoader.SortByPriority.ASC;

@Priority
public class DefaultKubernetesClientFactory implements AutoCloseable, KubernetesClientFactory {

    public static final String PORTFORWARD_FQDN_ENABLED_PROP = "portforward.fqdn.enabled";

    private final Config config;

    public DefaultKubernetesClientFactory() {
        this(new ConfigBuilder().build());
    }

    public DefaultKubernetesClientFactory(Config config) {
        this.config = config;
    }

    private final static ConcurrentHashMap<CloudAndNamespace, KubernetesClient> clientsMap = new ConcurrentHashMap<>();

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
            List<Fabric8ConfigBuilderAdapter> fabric8ConfigBuilderAdapters =
                    OrderedServiceLoader.loadAll(Fabric8ConfigBuilderAdapter.class, ASC);
            if (fabric8ConfigBuilderAdapters.isEmpty()) {
                throw new IllegalStateException("No Fabric8ConfigBuilderAdapter found");
            }
            ConfigBuilder configBuilder = new ConfigBuilder(config).withNamespace(cloudAndNamespace.getNamespace());
            for (Fabric8ConfigBuilderAdapter adapter : fabric8ConfigBuilderAdapters) {
                configBuilder = adapter.adapt(configBuilder);
            }
            config = configBuilder.build();

            List<Fabric8KubernetesClientBuilderAdapter> fabric8KubernetesClientBuilderAdapters =
                    OrderedServiceLoader.loadAll(Fabric8KubernetesClientBuilderAdapter.class, ASC);
            if (fabric8KubernetesClientBuilderAdapters.isEmpty()) {
                throw new IllegalStateException("No Fabric8KubernetesClientBuilderAdapter found");
            }
            KubernetesClientBuilder kubernetesClientBuilder = new KubernetesClientBuilder().withConfig(config);
            for (Fabric8KubernetesClientBuilderAdapter adapter : fabric8KubernetesClientBuilderAdapters) {
                kubernetesClientBuilder = adapter.adapt(kubernetesClientBuilder);
            }
            return kubernetesClientBuilder.build();
        });
    }

    @Override
    public String getCurrentContext() {
        return config.getCurrentContext().getName();
    }

    @Override
    public String getNamespace() {
        return config.getNamespace();
    }

    @Override
    public void close() {
        HashMap<CloudAndNamespace, KubernetesClient> copy = new HashMap<>(clientsMap);
        clientsMap.clear();
        copy.values().forEach(KubernetesClient::close);
    }
}
