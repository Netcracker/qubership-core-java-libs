package com.netcracker.cloud.junit.cloudcore.extension.service;

import com.netcracker.cloud.junit.cloudcore.extension.provider.LocalHostAddressGenerator;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.LocalPortForward;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class PortForwardService {

    private final Map<Endpoint, LocalPortForward> cache;
    protected final KubernetesClient kubernetesClient;
    @Getter
    protected boolean fqdn;
    @Getter
    protected boolean useFreeLocalPorts;

    public PortForwardService(KubernetesClient kubernetesClient, boolean fqdn, boolean useFreeLocalPorts) {
        this(kubernetesClient, new ConcurrentHashMap<>(), fqdn, useFreeLocalPorts);
    }

    public PortForwardService(KubernetesClient kubernetesClient, Map<Endpoint, LocalPortForward> cache,
                              boolean fqdn, boolean useFreeLocalPorts) {
        this.kubernetesClient = kubernetesClient;
        this.cache = cache;
        this.fqdn = fqdn;
        this.useFreeLocalPorts = useFreeLocalPorts;
    }

    public synchronized <T> T portForward(BasePortForwardParams<T> params) {
        String namespace = Optional.ofNullable(params.getNamespace()).orElseGet(kubernetesClient::getNamespace);
        String cloud = kubernetesClient.getMasterUrl().getHost();
        String name = params.getName();
        int targetPort = params.getPort();
        int localPort = useFreeLocalPorts ? 0 : targetPort;
        // i.e. my-svc.my-namespace.svc.cluster-domain.example
        String host = fqdn ? String.format("%s.svc.%s", params.host(namespace), cloud) : params.host(namespace);
        Endpoint endpoint = new Endpoint(host, targetPort);
        LocalPortForward portForward = cache.get(endpoint);
        if (portForward != null) {
            log.debug("Port forward for endpoint '{}:{}' already opened", host, targetPort);
        } else {
            InetAddress inetAddress;
            do {
                inetAddress = LocalHostAddressGenerator.getOrNext(host);
                try {
                    if (params instanceof PodPortForwardParams) {
                        portForward = kubernetesClient.pods().inNamespace(namespace).withName(name)
                                .portForward(targetPort, inetAddress, localPort);
                    } else if (params instanceof ServicePortForwardParams || params instanceof UrlPortForwardParams) {
                        portForward = kubernetesClient.services().inNamespace(namespace).withName(name)
                                .portForward(targetPort, inetAddress, localPort);
                    } else {
                        throw new IllegalArgumentException("Unsupported port forward params type: " + params.getClass().getName());
                    }
                } catch (Exception e) {
                    if (!(e.getCause() instanceof BindException)) {
                        throw e;
                    }
                }
            } while (portForward == null);
            LocalHostAddressGenerator.put(host, inetAddress);
            cache.put(endpoint, portForward);
            log.info("Created port forward {}:{} for endpoint {}:{}", host, localPort, host, targetPort);
            if (!ping(portForward.getLocalAddress(), Duration.ofSeconds(5))) { // todo check if that is working
                log.warn("Port forward ping for endpoint {}:{} failed", host, targetPort);
            }
        }
        return params.supply(new NetSocketAddress(host, portForward.getLocalPort()));
    }

    public void closePortForwards() {
        new HashSet<>(this.cache.keySet()).forEach(this::closePortForward);
        this.cache.clear();
    }

    public void closePortForward(Endpoint endpoint) {
        try {
            LocalPortForward portForward = this.cache.remove(endpoint);
            if (portForward != null) {
                closePortForward(portForward);
                LocalHostAddressGenerator.cleanup(endpoint.host(), () ->
                        this.cache.keySet().stream().noneMatch(end -> Objects.equals(end.host(), endpoint.host())));
                log.info("Closed port forward for endpoint: {}", endpoint);
            }
        } catch (Exception e) {
            log.warn("Error while closing portForwarder, e: {} - {}", e.getClass().getSimpleName(), e.getMessage() != null ? e.getMessage() : "");
        }
    }

    protected void closePortForward(LocalPortForward portForward) {
        try {
            portForward.close();
        } catch (IOException e) {
            log.warn("Error while closing portForwarder, e: {}", e.toString());
        }
    }

    protected boolean ping(InetAddress address, Duration timeout) {
        try {
            return address.isReachable((int) timeout.toMillis());
        } catch (IOException e) {
            return false;
        }
    }
}
