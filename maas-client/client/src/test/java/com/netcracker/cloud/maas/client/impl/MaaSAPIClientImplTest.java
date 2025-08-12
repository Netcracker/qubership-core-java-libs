package com.netcracker.cloud.maas.client.impl;

import org.junit.jupiter.api.Test;

class MaaSAPIClientImplTest {
    @Test
    void testConstructor() {
        // test that constructor runnable and doesn't throw any exception
        new MaaSAPIClientImpl(() -> "faketoken").getRabbitClient();
    }
}
