package com.netcracker.cloud.junit.cloudcore.extension.service;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

public class NetSocketAddress extends InetSocketAddress {

    public NetSocketAddress(String hostname, int port) {
        super(hostname, port);
    }

    public String getEndpoint() {
        return getHostString() + ":" + getPort();
    }

    public URL toHttpUrl() {
       return this.toUrl("http");
    }

    public URL toHttpsUrl() {
        return this.toUrl("https");
    }

    public URL toUrl(String protocol) {
        try {
            return URI.create(String.format("%s://%s/", protocol, getEndpoint())).toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(String.format("Failed to build URL from protocol: '%s' and endpoint: '%s'", protocol, getEndpoint()), e);
        }
    }
}
