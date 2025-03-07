package org.qubership.cloud.bluegreen.spring.config;

import org.qubership.cloud.bluegreen.api.service.GlobalMutexService;
import org.qubership.cloud.bluegreen.impl.service.ConsulGlobalMutexService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.qubership.cloud.bluegreen.spring.config.AbstractConsulTest.assertField;

@SpringBootTest(classes = {
        AbstractConsulTest.TestTokenStorageConfig.class,
        BlueGreenGlobalMutexConfiguration.class},
        properties = {"consul.url=http://test.consul:8500"})
public class BGGlobalMutexConfigTest {

    @Autowired
    GlobalMutexService globalMutexService;

    @Test
    void testGlobalMutexServiceBean() throws Exception {
        Assertions.assertNotNull(globalMutexService);
        Assertions.assertTrue(globalMutexService instanceof ConsulGlobalMutexService);

        ConsulGlobalMutexService consulGlobalMutexService = (ConsulGlobalMutexService) globalMutexService;
        assertField(consulGlobalMutexService, ConsulGlobalMutexService.class,
                "consulUrl", String.class, "http://test.consul:8500");
    }
}
