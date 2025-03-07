package org.qubership.cloud.bluegreen.spring.config;

import org.qubership.cloud.bluegreen.api.service.GlobalMutexService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {BlueGreenGlobalMutexConfiguration.class},
        properties = {"blue-green.global-mutex-service.enabled=false"})
public class BGGlobalMutexConfigDisabledTest {

    @Autowired(required = false)
    GlobalMutexService globalMutexService;

    @Test
    void testGlobalMutexServiceBean() {
        Assertions.assertNull(globalMutexService);
    }
}
