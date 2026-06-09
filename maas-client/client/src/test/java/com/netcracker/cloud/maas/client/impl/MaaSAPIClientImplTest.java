package com.netcracker.cloud.maas.client.impl;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MaaSAPIClientImplTest {
    @Test
    void testCoverage() throws Exception {
        MaaSAPIClientImpl client = new MaaSAPIClientImpl(() -> "faketoken");
        assertNotNull(client.getKafkaClient());
        assertNotNull(client.getRabbitClient());
        client.close();
    }
}
