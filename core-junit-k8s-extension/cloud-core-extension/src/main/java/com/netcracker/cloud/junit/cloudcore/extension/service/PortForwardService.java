package com.netcracker.cloud.junit.cloudcore.extension.service;

import com.netcracker.cloud.junit.cloudcore.extension.provider.LocalHostAddressGenerator;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.LocalPortForward;
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

    public PortForwardService(KubernetesClient kubernetesClient) {
        this(kubernetesClient, new ConcurrentHashMap<>());
    }

    public PortForwardService(KubernetesClient kubernetesClient, Map<Endpoint, LocalPortForward> cache) {
        this.kubernetesClient = kubernetesClient;
        this.cache = cache;
    }

    public synchronized NetSocketAddress portForward(PortForwardParams params) {
        String namespace = Optional.ofNullable(params.getNamespace()).orElseGet(kubernetesClient::getNamespace);
        String cloud = kubernetesClient.getMasterUrl().getHost();
        String serviceName = params.getServiceName();
        int targetPort = params.getPort();
        // i.e. my-svc.my-namespace.svc.cluster-domain.example
        String host = String.format("%s.%s.svc.%s", serviceName, namespace, cloud);
        Endpoint endpoint = new Endpoint(host, targetPort);
        LocalPortForward portForward = cache.get(endpoint);
        if (portForward != null) {
            log.debug("Port forward for endpoint '{}:{}' already opened", host, targetPort);
        } else {
            InetAddress inetAddress;
            do {
                inetAddress = LocalHostAddressGenerator.getOrNext(host);
                try {
                    portForward = kubernetesClient.services().inNamespace(namespace).withName(serviceName)
                            .portForward(targetPort, inetAddress, targetPort);
                } catch (Exception e) {
                    if (!(e.getCause() instanceof BindException)) {
                        throw e;
                    }
                }
            } while (portForward == null);
            LocalHostAddressGenerator.put(host, inetAddress);
            cache.put(endpoint, portForward);
            log.info("Created port forward for endpoint {}:{}", host, targetPort);
            if (!ping(portForward.getLocalAddress(), Duration.ofSeconds(5))) { // todo check if that is working
                log.warn("Port forward ping for endpoint {}:{} failed", host, targetPort);
            }
        }
        return new NetSocketAddress(host, portForward.getLocalPort());
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
