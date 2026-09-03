package com.netcracker.cloud.junit.cloudcore.extension.service;

import io.fabric8.kubernetes.client.LocalPortForward;
import lombok.Getter;

import java.net.InetAddress;
import java.time.Duration;

public interface PortForwardService {

    <T> T portForward(BasePortForwardParams<T> params);

    void closePortForwards();

    void closePortForward(Endpoint endpoint);

    void closePortForward(LocalPortForward portForward);

    boolean ping(InetAddress address, Duration timeout);

    boolean isFqdn();

    boolean isUseFreeLocalPorts();
}
