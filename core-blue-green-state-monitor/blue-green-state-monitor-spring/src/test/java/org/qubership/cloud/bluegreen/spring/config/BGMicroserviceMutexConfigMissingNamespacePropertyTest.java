package org.qubership.cloud.bluegreen.spring.config;

import org.qubership.cloud.bluegreen.api.service.MicroserviceMutexService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest(classes = {AbstractConsulTest.TestTokenStorageConfig.class, BlueGreenMicroserviceMutexConfiguration.class},
        properties = {
                "consul.url=http://test.consul:8500",
                "cloud.microservice.name=test-name",
                "pod.name=test-pod-name",
                "spring.main.lazy-initialization=true"})
class BGMicroserviceMutexConfigMissingNamespacePropertyTest {
    @Autowired
    private ApplicationContext context;

    @Test
    void testMicroserviceMutexServiceBean() {
        Assertions.assertThrows(Exception.class, () ->
                context.getBean(MicroserviceMutexService.class)
        );
    }
}
