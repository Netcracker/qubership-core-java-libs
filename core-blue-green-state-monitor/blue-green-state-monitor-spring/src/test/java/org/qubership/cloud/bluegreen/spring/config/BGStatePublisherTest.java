package org.qubership.cloud.bluegreen.spring.config;

import org.qubership.cloud.bluegreen.api.service.BlueGreenStatePublisher;
import org.qubership.cloud.bluegreen.impl.service.ConsulBlueGreenStatePublisher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {AbstractConsulTest.TestTokenStorageConfig.class, BlueGreenStatePublisherConfiguration.class},
        properties = {"cloud.microservice.namespace=test-namespace-1"})
class BGStatePublisherTest extends AbstractConsulTest {

    @Autowired
    BlueGreenStatePublisher blueGreenStatePublisher;

    @Test
    void testBlueGreenStatePublisherBean() {
        Assertions.assertNotNull(blueGreenStatePublisher);
        Assertions.assertTrue(blueGreenStatePublisher instanceof ConsulBlueGreenStatePublisher);
    }
}
