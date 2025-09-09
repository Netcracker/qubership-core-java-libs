package com.netcracker.cloud.junit.cloudcore.extension.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class NetSocketAddressTest {

    @Test
    void testGetEndpoint() {
        NetSocketAddress address = new NetSocketAddress("test-service", 8080);

        Assertions.assertEquals("test-service:8080", address.getEndpoint());
    }

    @Test
    void testToUrl() {
        NetSocketAddress address = new NetSocketAddress("test-service", 8080);

        Assertions.assertEquals("ftp://test-service:8080/", address.toUrl("ftp").toString());
    }

    @Test
    void testToHttpUrl() {
        NetSocketAddress address = new NetSocketAddress("test-service", 8080);

        Assertions.assertEquals("http://test-service:8080/", address.toHttpUrl().toString());
    }

    @Test
    void testToHttpsUrl() {
        NetSocketAddress address = new NetSocketAddress("test-service", 8080);

        Assertions.assertEquals("https://test-service:8080/", address.toHttpsUrl().toString());
    }
}
