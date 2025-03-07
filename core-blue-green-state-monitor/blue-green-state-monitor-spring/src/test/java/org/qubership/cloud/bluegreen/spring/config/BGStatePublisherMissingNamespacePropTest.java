package org.qubership.cloud.bluegreen.spring.config;

import org.qubership.cloud.bluegreen.api.service.BlueGreenStatePublisher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest(classes = {BlueGreenStatePublisherConfiguration.class},
        properties = {"spring.main.lazy-initialization=true"})
class BGStatePublisherMissingNamespacePropTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void testMicroserviceMutexServiceBean() {
        Assertions.assertThrows(Exception.class, () ->
                context.getBean(BlueGreenStatePublisher.class)
        );
    }
}
