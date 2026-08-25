package com.netcracker.cloud.junit.cloudcore.extension.provider;

import com.netcracker.cloud.junit.cloudcore.extension.annotations.Conditional;
import com.netcracker.cloud.junit.cloudcore.extension.annotations.Priority;
import com.netcracker.cloud.junit.cloudcore.extension.service.PortForwardService;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static com.netcracker.cloud.junit.cloudcore.extension.provider.PortForwardProvider.IN_K8S;

@Slf4j
@Conditional(IN_K8S)
@Priority
public class DirectCommunicationServiceManager implements PortForwardServiceManager {

    protected static Map<PortForwardConfig, PortForwardService> portForwardServiceMap = new ConcurrentHashMap<>();

    @Override
    public PortForwardService getPortForwardService(PortForwardConfig config) {
        return portForwardServiceMap.computeIfAbsent(config, c ->
                new PortForwardService(null, false, false, true));
    }

    @Override
    public void close() {
        portForwardServiceMap.values().stream().filter(Objects::nonNull).forEach(PortForwardService::closePortForwards);
        portForwardServiceMap.clear();
    }
}
