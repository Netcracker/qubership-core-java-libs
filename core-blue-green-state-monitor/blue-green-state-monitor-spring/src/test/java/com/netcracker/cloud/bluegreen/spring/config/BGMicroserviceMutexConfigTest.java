package com.netcracker.cloud.bluegreen.spring.config;

import com.netcracker.cloud.bluegreen.api.service.MicroserviceMutexService;
import com.netcracker.cloud.bluegreen.impl.service.ConsulMicroserviceMutexService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.InetAddress;

import static com.netcracker.cloud.bluegreen.spring.config.AbstractConsulTest.assertField;

@SpringBootTest(classes = {AbstractConsulTest.TestTokenStorageConfig.class, BlueGreenMicroserviceMutexConfiguration.class},
        properties = {
                "consul.url=http://test.consul:8500",
                "cloud.microservice.namespace=test-namespace-1",
                "cloud.microservice.name=test-name"})
class BGMicroserviceMutexConfigTest {

    @Autowired
    MicroserviceMutexService microserviceMutexService;

    @Test
    void testMicroserviceMutexServiceBean() throws Exception {
        Assertions.assertNotNull(microserviceMutexService);
        Assertions.assertTrue(microserviceMutexService instanceof ConsulMicroserviceMutexService);

        ConsulMicroserviceMutexService consulMicroserviceMutexService = (ConsulMicroserviceMutexService) microserviceMutexService;
        assertField(consulMicroserviceMutexService, ConsulMicroserviceMutexService.class, "consulUrl", String.class, "http://test.consul:8500");
        assertField(consulMicroserviceMutexService, ConsulMicroserviceMutexService.class, "microserviceName", String.class, "test-name");
        assertField(consulMicroserviceMutexService, ConsulMicroserviceMutexService.class, "podName", String.class, InetAddress.getLocalHost().getHostName());
    }
}
