package org.qubership.cloud.bluegreen.spring.config;

import org.qubership.cloud.bluegreen.api.service.MicroserviceMutexService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {AbstractConsulTest.TestTokenStorageConfig.class, BlueGreenMicroserviceMutexConfiguration.class},
        properties = {"blue-green.microservice-mutex-service.enabled=false"})
class BGMicroserviceMutexConfigDisabledTest {

    @Autowired(required = false)
    MicroserviceMutexService microserviceMutexService;

    @Test
    void testMicroserviceMutexServiceBean() {
        Assertions.assertNull(microserviceMutexService);
    }
}
