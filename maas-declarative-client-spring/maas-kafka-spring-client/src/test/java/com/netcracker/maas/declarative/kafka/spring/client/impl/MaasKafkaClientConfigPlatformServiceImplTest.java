package org.qubership.maas.declarative.kafka.spring.client.impl;

import org.qubership.maas.declarative.kafka.client.impl.common.constant.MaasKafkaCommonConstants;
import org.qubership.maas.declarative.kafka.spring.client.config.MaasKafkaClientConfigKeeper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MaasKafkaClientConfigPlatformServiceImplTest {

    @Test
    void testConfigurationParsers() {
        var map = new MaasKafkaClientConfigKeeper();
        map.setClient(Map.of("consumer",
                Map.of("orders",
                        Map.of(
                        "string", "value",
                        "int", "1",
                        "double", "1.0",
                        "boolean", "true"
                        )
                )
            ));

        var config = new MaasKafkaClientConfigPlatformServiceImpl(map);
        var subset = config.getClientConfigByPrefix(MaasKafkaCommonConstants.CONSUMER_PREFIX + "orders");

        var expected = Map.of("string", "value", "int", 1, "double", 1.0, "boolean", true);
        assertEquals(expected, subset);
    }

}