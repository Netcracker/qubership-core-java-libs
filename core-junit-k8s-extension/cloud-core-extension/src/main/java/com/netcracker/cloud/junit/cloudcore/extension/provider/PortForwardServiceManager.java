package com.netcracker.cloud.junit.cloudcore.extension.provider;

import com.netcracker.cloud.junit.cloudcore.extension.service.PortForwardService;

public interface PortForwardServiceManager extends AutoCloseable {

    PortForwardService getPortForwardService(PortForwardConfig config);

    void close();

}
