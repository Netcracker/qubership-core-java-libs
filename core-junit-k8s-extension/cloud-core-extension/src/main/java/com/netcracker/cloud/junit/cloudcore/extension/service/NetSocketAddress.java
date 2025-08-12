package com.netcracker.cloud.junit.cloudcore.extension.service;

import java.net.InetSocketAddress;

public class NetSocketAddress extends InetSocketAddress {

    public NetSocketAddress(String hostname, int port) {
        super(hostname, port);
    }

    public String getEndpoint() {
        return getHostString() + ":" + getPort();
    }
}
