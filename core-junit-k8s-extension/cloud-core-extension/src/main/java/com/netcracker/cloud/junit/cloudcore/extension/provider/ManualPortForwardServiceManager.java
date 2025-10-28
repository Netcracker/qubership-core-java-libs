package com.netcracker.cloud.junit.cloudcore.extension.provider;

import com.netcracker.cloud.junit.cloudcore.extension.annotations.Priority;
import com.netcracker.cloud.junit.cloudcore.extension.service.PortForwardService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Function;

@Slf4j
@Priority(0)
public class ManualPortForwardServiceManager implements PortForwardServiceManager {

    @Setter
    private static Function<PortForwardConfig, PortForwardService> function;

    @Getter
    private static boolean closed;

    @Override
    public PortForwardService getPortForwardService(PortForwardConfig config) {
        if (function == null) {
            throw new IllegalStateException("No function provided");
        }
        return function.apply(config);
    }

    @Override
    public void close() {
        closed = true;
    }
}
