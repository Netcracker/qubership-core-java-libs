package com.netcracker.cloud.junit.cloudcore.extension.provider;

import com.netcracker.cloud.junit.cloudcore.extension.annotations.Priority;
import com.netcracker.cloud.junit.cloudcore.extension.client.KubernetesClientFactory;
import com.netcracker.cloud.junit.cloudcore.extension.service.PortForwardService;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Priority
public class DefaultPortForwardServiceManager implements PortForwardServiceManager {

    protected static Map<PortForwardConfig, PortForwardService> portForwardServiceMap = new ConcurrentHashMap<>();
    public static String PORTFORWARD_FQDN_ENABLED_PROP = "portforward.fqdn.hosts.enabled";

    @Override
    public PortForwardService getPortForwardService(PortForwardConfig config) {
        return portForwardServiceMap.computeIfAbsent(config, c -> {
            KubernetesClientFactory kubernetesClientFactory = OrderedServiceLoader.load(KubernetesClientFactory.class)
                    .orElseThrow(() -> new IllegalStateException("No KubernetesClientFactory implementation found"));
            KubernetesClient kubernetesClient = kubernetesClientFactory.getKubernetesClient(c.getCloud(), c.getNamespace());
            boolean fqdnFromProp = Boolean.parseBoolean(System.getProperty(PORTFORWARD_FQDN_ENABLED_PROP, "false"));
            Pattern cloudPropPattern = Pattern.compile("^clouds\\.(?<name>[^.]+)\\.name$");
            Set<String> clouds = System.getProperties().keySet().stream()
                    .map(o -> cloudPropPattern.matcher(o.toString()))
                    .filter(Matcher::matches)
                    .map(m -> m.group("name"))
                    .collect(Collectors.toSet());
            boolean fqdn = fqdnFromProp || clouds.size() > 1;
            return new PortForwardService(kubernetesClient, fqdn);
        });
    }

    @Override
    public void close() {
        portForwardServiceMap.values().stream().filter(Objects::nonNull).forEach(PortForwardService::closePortForwards);
        portForwardServiceMap.clear();
    }
}
