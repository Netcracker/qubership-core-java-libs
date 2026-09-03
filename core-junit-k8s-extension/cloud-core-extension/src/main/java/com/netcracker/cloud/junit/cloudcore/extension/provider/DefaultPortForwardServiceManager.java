package com.netcracker.cloud.junit.cloudcore.extension.provider;

import com.netcracker.cloud.junit.cloudcore.extension.annotations.Priority;
import com.netcracker.cloud.junit.cloudcore.extension.client.KubernetesClientFactory;
import com.netcracker.cloud.junit.cloudcore.extension.service.InsideK8S;
import com.netcracker.cloud.junit.cloudcore.extension.service.OutsideK8S;
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
    public final static String PORTFORWARD_FQDN_ENABLED_PROP = "portforward.fqdn.hosts.enabled";
    public final static String USE_FREE_LOCAL_PORTS_PROP = "portforward.use.free.local.ports";
    public final static Boolean in_k8s = "true".equalsIgnoreCase(System.getenv("IN_K8S"));

    @Override
    public PortForwardService getPortForwardService(PortForwardConfig config) {
        return portForwardServiceMap.computeIfAbsent(config, c -> {
            if (in_k8s){
                return new InsideK8S();
            }
            KubernetesClientFactory kubernetesClientFactory = OrderedServiceLoader.load(KubernetesClientFactory.class)
                    .orElseThrow(() -> new IllegalStateException("No KubernetesClientFactory implementation found"));
            KubernetesClient kubernetesClient;
            kubernetesClient = kubernetesClientFactory.getKubernetesClient(c.getCloud(), c.getNamespace());
            boolean fqdnFromProp = Boolean.parseBoolean(System.getProperty(PORTFORWARD_FQDN_ENABLED_PROP, "false"));
            boolean useFreeLocalPorts = Boolean.parseBoolean(System.getProperty(USE_FREE_LOCAL_PORTS_PROP, "false"));
            Pattern cloudPropPattern = Pattern.compile("^clouds\\.(?<name>[^.]+)\\.name$");
            Set<String> clouds = System.getProperties().keySet().stream()
                    .map(o -> cloudPropPattern.matcher(o.toString()))
                    .filter(Matcher::matches)
                    .map(m -> m.group("name"))
                    .collect(Collectors.toSet());
            boolean fqdn = fqdnFromProp || clouds.size() > 1;
            return new OutsideK8S(kubernetesClient, fqdn, useFreeLocalPorts);
        });
    }

    @Override
    public void close() {
        portForwardServiceMap.values().stream().filter(Objects::nonNull).forEach(PortForwardService::closePortForwards);
        portForwardServiceMap.clear();
    }
}
