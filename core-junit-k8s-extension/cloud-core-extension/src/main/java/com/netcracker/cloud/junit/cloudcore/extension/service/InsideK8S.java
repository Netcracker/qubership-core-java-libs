package com.netcracker.cloud.junit.cloudcore.extension.service;

import com.netcracker.cloud.junit.cloudcore.extension.provider.LocalHostAddressGenerator;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.LocalPortForward;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.sound.sampled.Port;
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
public class InsideK8S implements PortForwardService {

    public InsideK8S() {
    }

    @Override
    public synchronized <T> T portForward(BasePortForwardParams<T> params) {
        String name = params.getName();
        int targetPort = params.getPort();
        return params.supply(new NetSocketAddress(name, targetPort));
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

    @Override
    public boolean ping(InetAddress address, Duration timeout) {
        try {
            return address.isReachable((int) timeout.toMillis());
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public boolean isFqdn() {
        return false;
    }

    @Override
    public boolean isUseFreeLocalPorts() {
        return false;
    }
}
