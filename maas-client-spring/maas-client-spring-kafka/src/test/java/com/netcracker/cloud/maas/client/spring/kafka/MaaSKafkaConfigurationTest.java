package com.netcracker.cloud.maas.client.spring.kafka;

import com.netcracker.cloud.maas.client.api.kafka.KafkaMaaSClient;
import com.netcracker.cloud.maas.spring.MaaSClientConfig;
import com.netcracker.cloud.maas.spring.localdev.MaasLocalDevConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {
        MaaSClientConfig.class,
        MaasLocalDevConfig.class,
        MaaSKafkaConfiguration.class},
        properties = {
                "spring.cloud.consul.config.enabled=false",
                "maas.local-dev.enabled=true"})
class MaaSKafkaConfigurationTest {

    @Autowired
    KafkaMaaSClient kafkaMaaSClient;

    @Test
    void testKafkaMaaSClientCreated() {
        Assertions.assertNotNull(kafkaMaaSClient);
    }
}
