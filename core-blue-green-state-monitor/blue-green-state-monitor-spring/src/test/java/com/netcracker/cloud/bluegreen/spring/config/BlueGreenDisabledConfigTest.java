package com.netcracker.cloud.bluegreen.spring.config;

import com.netcracker.cloud.bluegreen.api.service.BlueGreenStatePublisher;
import com.netcracker.cloud.bluegreen.impl.service.InMemoryBlueGreenStatePublisher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {InMemoryConfig.class,
        BlueGreenGlobalMutexConfiguration.class,
        BlueGreenMicroserviceMutexConfiguration.class,
        BlueGreenStatePublisherConfiguration.class},
        properties = {"blue-green.enabled=false"},
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class BlueGreenDisabledConfigTest {

    @Autowired
    BlueGreenStatePublisher blueGreenStatePublisher;
//    @Autowired
//    GlobalMutexService globalMutexService;
//    @Autowired
//    MicroserviceMutexService microserviceMutexService;

    @Test
    void testBlueGreenStatePublisherBean() {
        Assertions.assertNotNull(blueGreenStatePublisher);
        Assertions.assertTrue(blueGreenStatePublisher instanceof InMemoryBlueGreenStatePublisher);
    }

//    @Test
//    void testGlobalMutexServiceBean() {
//        Assertions.assertNotNull(globalMutexService);
//        Assertions.assertTrue(globalMutexService instanceof InMemoryGlobalMutexService);
//    }
//
//    @Test
//    void testMicroserviceMutexServiceBean() {
//        Assertions.assertNotNull(microserviceMutexService);
//        Assertions.assertTrue(microserviceMutexService instanceof InMemoryMicroserviceMutexService);
//    }

}
