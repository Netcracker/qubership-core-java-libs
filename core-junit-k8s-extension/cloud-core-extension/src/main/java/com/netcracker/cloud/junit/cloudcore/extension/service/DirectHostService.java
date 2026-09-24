package com.netcracker.cloud.junit.cloudcore.extension.service;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.LocalPortForward;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class DirectHostService  extends PortForwardService {

    protected final KubernetesClient kubernetesClient;

    public DirectHostService(KubernetesClient kubernetesClient) {
        super(kubernetesClient, false, false);
        this.kubernetesClient = kubernetesClient;
    }

    @Override
    public synchronized <T> T portForward(BasePortForwardParams<T> params) {
        int targetPort = params.getPort();
        String namespace = Optional.ofNullable(params.getNamespace()).orElseGet(kubernetesClient::getNamespace);
        String host = params.host(namespace);
        return params.supply(new NetSocketAddress(host, targetPort));
    }

    @Override
    public boolean isFqdn() {
        return false;
    }

    @Override
    public boolean isUseFreeLocalPorts() {
        return false;
    }

    @Override
    public void closePortForwards() {
        /* No need to close port forward that does not exist */
    }

    @Override
    public void closePortForward(Endpoint endpoint) {
        /* No need to close port forward that does not exist */
    }

    @Override
    public void closePortForward(LocalPortForward portForward) {
        /* No need to close port forward that does not exist */
    }
}
