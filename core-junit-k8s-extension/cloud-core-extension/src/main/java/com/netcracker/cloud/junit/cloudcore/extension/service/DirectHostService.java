package com.netcracker.cloud.junit.cloudcore.extension.service;

import io.fabric8.kubernetes.client.LocalPortForward;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DirectHostService  extends PortForwardService {
    public DirectHostService() {
        super(null, false, false);
    }

    @Override
    public synchronized <T> T portForward(BasePortForwardParams<T> params) {
        return params.supply(new NetSocketAddress(params.getName(), params.getPort()));
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
    }

    @Override
    public void closePortForward(Endpoint endpoint) {
    }

    @Override
    public void closePortForward(LocalPortForward portForward) {
    }
}
