package com.netcracker.cloud.maas.client.impl;

import org.junit.jupiter.api.Test;

class MaaSAPIClientImplTest {
    @Test
    void testConstructor() {
        // test that constructor runnable and doesn't throw any exception
        System.setProperty(Env.PROP_API_URL, "http://localhost:8080");
        new MaaSAPIClientImpl(() -> "faketoken").getRabbitClient();
        System.clearProperty(Env.PROP_API_URL);
    }
}
